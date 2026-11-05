package com.waypoint.app

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.AndroidTtsStatusListener
import com.stadiamaps.ferrostar.core.SpokenInstructionObserver
import kotlinx.coroutines.flow.StateFlow
import uniffi.ferrostar.SpokenInstruction
import java.util.Locale

internal enum class GuidanceVoiceStatus(private val english: String? = null, private val hebrew: String? = null) {
    IDLE, STARTING, READY,
    ENGINE_UNAVAILABLE("Voice could not start. Check the preferred text-to-speech engine in Android settings, then turn voice off and on to retry. Visual guidance is active.",
        "ההנחיות הקוליות לא הופעלו. בדקו את מנוע ההקראה המועדף בהגדרות Android, ואז כבו והפעילו את הקול כדי לנסות שוב. ההנחיות על המסך פעילות."),
    LANGUAGE_UNAVAILABLE("No English guidance voice is available. Choose or download an English voice in Android text-to-speech settings, then turn voice off and on to retry. Visual guidance is active.",
        "אין קול עברי להנחיות. בהגדרות ההקראה של Android בחרו מנוע שתומך בעברית (למשל „שירותי דיבור של Google”) והורידו קול עברי, ואז כבו והפעילו את הקול כדי לנסות שוב. ההנחיות על המסך פעילות."),
    PLAYBACK_FAILED("Voice playback failed. Check Android text-to-speech settings and navigation volume, then turn voice off and on to retry. Visual guidance is active.",
        "השמעת הקול נכשלה. בדקו את הגדרות ההקראה של Android ואת עוצמת הקול של הניווט, ואז כבו והפעילו את הקול כדי לנסות שוב. ההנחיות על המסך פעילות."),
    ;
    /** Read at display time so a language change never shows a stale message. */
    val problem: String? get() = english?.let { tr(it, hebrew ?: it) }
}

/** Handles asynchronous engine startup without replaying a backlog of obsolete turns. */
internal class GuidanceVoice(
    private val delegate: SpokenInstructionObserver,
    private val initialize: ((GuidanceVoiceStatus) -> Unit) -> Unit,
    private val release: () -> Unit,
    private val onStatus: (GuidanceVoiceStatus) -> Unit,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) : SpokenInstructionObserver {
    @Volatile var status = GuidanceVoiceStatus.IDLE
        private set
    private var generation = 0L
    private var closed = false
    private var pending: Pair<SpokenInstruction, Long>? = null
    override val muteState: StateFlow<Boolean> get() = delegate.muteState

    @Synchronized fun prepare() {
        if (closed || status == GuidanceVoiceStatus.STARTING || status == GuidanceVoiceStatus.READY) return
        val ticket = ++generation
        status = GuidanceVoiceStatus.STARTING
        onStatus(status)
        try {
            initialize { next -> initialized(ticket, next) }
        } catch (_: Exception) {
            initialized(ticket, GuidanceVoiceStatus.ENGINE_UNAVAILABLE)
        }
    }

    @Synchronized private fun initialized(ticket: Long, next: GuidanceVoiceStatus) {
        if (closed || ticket != generation) return
        status = next
        onStatus(next)
        val waiting = pending
        pending = null
        if (next == GuidanceVoiceStatus.READY && !isMuted && waiting != null &&
            nowMillis() - waiting.second in 0..10_000) {
            delegate.onSpokenInstructionTrigger(waiting.first)
        }
    }

    @Synchronized override fun onSpokenInstructionTrigger(spokenInstruction: SpokenInstruction) {
        if (closed || isMuted) return
        when (status) {
            GuidanceVoiceStatus.READY -> delegate.onSpokenInstructionTrigger(spokenInstruction)
            GuidanceVoiceStatus.STARTING -> pending = spokenInstruction to nowMillis()
            else -> Unit
        }
    }

    @Synchronized override fun stopAndClearQueue() {
        pending = null
        delegate.stopAndClearQueue()
    }

    @Synchronized override fun setMuted(isMuted: Boolean) {
        delegate.setMuted(isMuted)
        if (isMuted) stopAndClearQueue() else prepare()
    }

    @Synchronized fun shutdown() {
        closed = true
        generation++
        stopAndClearQueue()
        release()
        status = GuidanceVoiceStatus.IDLE
    }
}

internal fun androidGuidanceVoice(context: Context, world: () -> World? = { null },
                                  onStatus: (GuidanceVoiceStatus) -> Unit): GuidanceVoice {
    val delegate = AndroidTtsObserver(context)
    val speaker = WorldGuidanceSpeaker(delegate, world, tone = { style ->
        // Preserve Android's speech preferences as the baseline. Unsupported
        // tone controls must never prevent a valid turn from being spoken.
        delegate.tts?.let { tts ->
            runCatching {
                val pitch = Settings.Secure.getInt(context.contentResolver,Settings.Secure.TTS_DEFAULT_PITCH,100)
                    .takeIf { it in 10..600 }?.div(100f) ?: 1f
                val rate = Settings.Secure.getInt(context.contentResolver,Settings.Secure.TTS_DEFAULT_RATE,100)
                    .takeIf { it in 10..600 }?.div(100f) ?: 1f
                if(tts.setPitch(pitch * (style?.pitch ?: 1f)) == TextToSpeech.ERROR)tts.setPitch(pitch)
                if(tts.setSpeechRate(rate * (style?.pace ?: 1f)) == TextToSpeech.ERROR)tts.setSpeechRate(rate)
            }
        }
    })
    val main = Handler(Looper.getMainLooper())
    return GuidanceVoice(speaker, initialize = { report ->
        // Ferrostar 0.59.0 does not start TTS in its constructor. Release failed
        // instances before retrying; install the listener before explicitly starting.
        delegate.statusObserver = null
        delegate.shutdown()
        delegate.statusObserver = object : AndroidTtsStatusListener {
            override fun onTtsInitialized(tts: TextToSpeech?, status: Int) {
                main.post {
                    val result = if (tts == null || status != TextToSpeech.SUCCESS) {
                        GuidanceVoiceStatus.ENGINE_UNAVAILABLE
                    } else runCatching {
                        tts.setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        // Route instructions follow the app language. Prefer the user's
                        // dialect, then the engine default and any installed variant.
                        val preferred = context.resources.configuration.locales[0]
                        val wanted = if (AppLanguage.isHebrew) setOf("iw", "he") else setOf("en")
                        val locales = buildList {
                            if (preferred.language in wanted) add(preferred)
                            tts.defaultVoice?.locale?.takeIf { it.language in wanted }?.let(::add)
                            add(AppLanguage.speechLocale)
                            if (AppLanguage.isHebrew) add(Locale.forLanguageTag("iw-IL"))
                            addAll(tts.availableLanguages.orEmpty().filter { it.language in wanted }.sortedBy { it.toLanguageTag() })
                        }.distinct()
                        if (locales.any { tts.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }) GuidanceVoiceStatus.READY
                        else GuidanceVoiceStatus.LANGUAGE_UNAVAILABLE
                    }.getOrDefault(GuidanceVoiceStatus.ENGINE_UNAVAILABLE)
                    report(result)
                }
            }
            override fun onTtsShutdownAndRelease() = Unit
            override fun onTtsSpeakError(utteranceId: String, status: Int) {
                main.post { report(GuidanceVoiceStatus.PLAYBACK_FAILED) }
            }
        }
        delegate.start()
    }, release = {
        delegate.statusObserver = null
        delegate.shutdown()
    }, onStatus = onStatus)
}
