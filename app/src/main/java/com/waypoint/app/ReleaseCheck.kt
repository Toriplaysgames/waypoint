package com.waypoint.app

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal fun releaseCheckCall(client:OkHttpClient):Call = client.newCall(Request.Builder()
    .url(ReleaseUpdate.API).header("Accept","application/vnd.github+json")
    .header("X-GitHub-Api-Version","2022-11-28")
    .header("User-Agent","Waypoint/${BuildConfig.VERSION_NAME}").build())

/** Shared bounded response/parser for automatic alerts and foreground update checks. */
internal fun readReleaseCheck(call:Call):ReleaseUpdate? = call.execute().use { response ->
    when(response.code) {
        404 -> throw IOException(tr("No public release is available yet. Try again after the first build is published.","עדיין אין גרסה ציבורית. נסו שוב אחרי שתפורסם הגרסה הראשונה."))
        403,429 -> throw IOException(tr("GitHub's update limit was reached. Please try again later.","הגענו למגבלת הבדיקות של GitHub. נסו שוב מאוחר יותר."))
    }
    if(!response.isSuccessful)throw IOException(tr("GitHub is unavailable (${response.code}). Please try again.","GitHub אינו זמין (${response.code}). נסו שוב."))
    val body=response.body ?: throw IOException(tr("GitHub returned an empty response.","GitHub החזיר תשובה ריקה."))
    val bytes=body.byteStream().use { input ->
        val output=java.io.ByteArrayOutputStream()
        val buffer=ByteArray(8192)
        while(output.size()<=1_000_000) {
            val count=input.read(buffer)
            if(count<0)break
            output.write(buffer,0,count)
        }
        output.toByteArray()
    }
    require(bytes.size<=1_000_000) { tr("The release response is too large.","התשובה על הגרסה גדולה מדי.") }
    ReleaseUpdate.parse(bytes.toString(Charsets.UTF_8),BuildConfig.VERSION_CODE)
}
