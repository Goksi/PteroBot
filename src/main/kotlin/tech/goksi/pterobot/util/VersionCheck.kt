package tech.goksi.pterobot.util

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.*
import tech.goksi.pterobot.manager.EmbedManager.replace
import java.io.IOException

object VersionCheck {
    private val logger by SLF4J

    private const val API_URL = "https://api.github.com/repos/Goksi/PteroBot/releases/latest"
    private const val VERSION = "@VERSION@"

    fun checkVersion() {
        //val currentVer = VERSION.replace(".", "").toInt()
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(API_URL)
            .header("accept", "application/vnd.github+json")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.error("Error while requesting version !", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) logger.error("Error while requesting version, unexpected status code: $response")
                    val fetchedVer = DataObject.fromJson(response.body!!.byteStream())
                        .getString("tag_name")
                        .replace("v" to "", "." to "").toInt()
                    logger.info(fetchedVer.toString())
                }
            }
        })
    }
}