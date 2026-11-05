package com.waypoint.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object Current : UpdateStatus
    data class Available(val release: ReleaseUpdate) : UpdateStatus
    data class Downloading(val release: ReleaseUpdate, val percent: Int) : UpdateStatus
    data class Ready(val release: ReleaseUpdate, val file: File) : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}

class UpdateFileProvider : FileProvider()

/** Checks on foreground entry; downloads and installation remain explicit user actions. */
class AppUpdater(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    @Volatile private var call: Call? = null
    private val directory get() = File(context.cacheDir, "updates").apply { mkdirs() }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(5, TimeUnit.MINUTES)
        .addNetworkInterceptor { chain ->
            if (!ReleaseUpdate.trustedDownloadHost(chain.request().url.toString())) throw IOException(tr("Untrusted update server.","שרת העדכונים אינו מהימן."))
            chain.proceed(chain.request())
        }.build()

    fun checkOnLaunch() {
        when (mutable.value) {
            is UpdateStatus.Downloading, is UpdateStatus.Ready -> return
            else -> check()
        }
    }

    fun check() {
        if (job?.isCompleted == false) return
        val previousOffer = mutable.value as? UpdateStatus.Available
        mutable.value = UpdateStatus.Checking
        job = scope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    readReleaseCheck(releaseCheckCall(client).also { call=it })
                }
                mutable.value = release?.let(UpdateStatus::Available) ?: UpdateStatus.Current
                release?.let { (context.applicationContext as WaypointApplication).updateAlerts.notifyIfNew(it) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { currentCoroutineContext().ensureActive(); mutable.value = previousOffer ?: UpdateStatus.Failed(friendlyError(e)) }
            finally { call = null }
        }
    }

    fun download(release: ReleaseUpdate) {
        if (job?.isCompleted == false) return
        mutable.value = UpdateStatus.Downloading(release, 0)
        job = scope.launch {
            val partial = File(directory, "update.part")
            val target = File(directory, "Waypoint.apk")
            try {
                withContext(Dispatchers.IO) {
                    target.delete(); partial.delete()
                    require(directory.usableSpace > release.size + 10 * 1024 * 1024) { tr("Not enough free space. Free some storage and try again.","אין מספיק מקום פנוי. פנו מקום באחסון ונסו שוב.") }
                    val digest = MessageDigest.getInstance("SHA-256")
                    val request = Request.Builder().url(release.url).header("User-Agent", "Waypoint/${BuildConfig.VERSION_NAME}").build()
                    client.newCall(request).also { call = it }.execute().use { response ->
                        if (!response.isSuccessful) throw IOException(tr("The download failed (${response.code}). Please try again.","ההורדה נכשלה (${response.code}). נסו שוב."))
                        val body = response.body ?: throw IOException(tr("The download was empty.","הקובץ שהורד ריק."))
                        var received = 0L
                        body.byteStream().use { input -> partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                received += count
                                require(received <= release.size && received <= ReleaseUpdate.MAX_BYTES) { tr("The update is larger than expected.","העדכון גדול מהצפוי.") }
                                output.write(buffer, 0, count); digest.update(buffer, 0, count)
                                currentCoroutineContext().ensureActive()
                                mutable.value = UpdateStatus.Downloading(release, (received * 100 / release.size).toInt())
                            }
                        } }
                        require(received == release.size) { tr("The download was incomplete. Please try again.","ההורדה לא הושלמה. נסו שוב.") }
                    }
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    require(actual == release.sha256) { tr("Update verification failed. The file was discarded; please try again.","אימות העדכון נכשל. הקובץ נמחק; נסו שוב.") }
                    verifyApk(partial, release)
                    check(partial.renameTo(target)) { tr("The update could not be saved. Please try again.","לא הצלחנו לשמור את העדכון. נסו שוב.") }
                }
                mutable.value = UpdateStatus.Ready(release, target)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { currentCoroutineContext().ensureActive(); mutable.value = UpdateStatus.Failed(friendlyError(e)) }
            finally { partial.delete(); call = null }
        }
    }

    fun cancel() {
        val release = (mutable.value as? UpdateStatus.Downloading)?.release
        call?.cancel(); job?.cancel()
        mutable.value = release?.let(UpdateStatus::Available) ?: UpdateStatus.Idle
    }

    /** Android still owns the permission screen and the final installation confirmation. */
    fun install(activity: Context, ready: UpdateStatus.Ready): String? = try {
        verifyApk(ready.file, ready.release)
        if (!context.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            tr("Allow updates from Waypoint, then return and tap Install update.","אפשרו עדכונים מ-Waypoint, ואז חזרו והקישו על „התקנת העדכון”.")
        } else {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", ready.file)
            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            null
        }
    } catch (e: Exception) { tr("Android couldn't open this update. Check installation permissions and try again.","Android לא הצליח לפתוח את העדכון. בדקו את הרשאות ההתקנה ונסו שוב.") }

    @Suppress("DEPRECATION")
    private fun verifyApk(file: File, release: ReleaseUpdate) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = requireNotNull(context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)) { tr("The download isn't a valid Android app.","הקובץ שהורד אינו אפליקציית Android תקינה.") }
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(candidate.packageName == context.packageName && candidate.versionName == release.version) { tr("The download is not this Waypoint release.","הקובץ שהורד אינו הגרסה הזאת של Waypoint.") }
        val code = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        require(code == release.versionCode.toLong() && code > BuildConfig.VERSION_CODE) { tr("The download is not a newer version.","הקובץ שהורד אינו גרסה חדשה יותר.") }
        fun signatures(info: PackageInfo): Set<String> = (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            .orEmpty().map { it.toCharsString() }.toSet()
        val expected = signatures(installed)
        require(expected.isNotEmpty() && signatures(candidate) == expected) { tr("This update has a different signing identity and cannot replace your app.","לעדכון יש חתימה שונה, ולכן הוא לא יכול להחליף את האפליקציה שלך.") }
    }
    private fun friendlyError(e: Exception) = when (e) {
        is java.net.UnknownHostException -> tr("You're offline. Connect to the internet and check again.","אין חיבור לאינטרנט. התחברו ובדקו שוב.")
        is java.net.SocketTimeoutException -> tr("The connection timed out. Please try again.","תם הזמן לחיבור. נסו שוב.")
        else -> e.message?.take(220) ?: tr("The update could not be checked. Please try again.","לא הצלחנו לבדוק אם יש עדכון. נסו שוב.")
    }
}
