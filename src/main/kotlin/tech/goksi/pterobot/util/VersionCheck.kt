package tech.goksi.pterobot.util

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.*
import java.io.IOException
import java.util.Properties

object VersionCheck {
    private val logger by SLF4J

    private const val API_URL = "https://api.github.com/repos/Goksi/PteroBot/releases/latest"

    fun checkVersion() {
        val properties = Properties()
            .apply { this.load(VersionCheck::class.java.classLoader.getResourceAsStream("version.properties")) }
        val currentVer = properties.getProperty("version")!!
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
                    if (!response.isSuccessful) {
                        logger.error("Error while requesting version, unexpected status code: $response")
                        return
                    }
                    val fetchedVer = DataObject.fromJson(response.body!!.byteStream())
                        .getString("tag_name")

                    if (compareVersions(fetchedVer.replace("v", ""), currentVer) > 0)
                        logger.warn("You are not running latest version of PteroBot ! Latest: $fetchedVer Current: v$currentVer")
                    else logger.info("You are running latest version of PteroBot !")
                }

            }
        })
    }

    private fun compareVersions(version1: String, version2: String): Int {
        var result = 0

        val ver1List = version1.split(".")
        val ver2List = version2.split(".")

        for (i in 0..2) {
            val v1 = ver1List[i].toInt()
            val v2 = ver2List[i].toInt()
            val compare = v1.compareTo(v2)
            if(compare != 0){
                result = compare
                break;
            }
        }
        return result
    }
}