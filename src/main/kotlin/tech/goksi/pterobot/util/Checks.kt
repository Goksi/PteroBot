package tech.goksi.pterobot.util

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.utils.MiscUtil
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.*
import tech.goksi.pterobot.entities.SemVer
import java.io.IOException
import java.util.*

object Checks {
    val logger by SLF4J
    inline fun <T> checkInput(
        currentInput: T,
        defaultValue: T,
        message: String,
        expression: () -> T
    ): Pair<T, Boolean> {
        var fixed: T
        var edited = false
        if (currentInput == null || currentInput == defaultValue) {
            edited = true
            do {
                logger.info(message)
                fixed = expression.invoke() ?: defaultValue
            } while (fixed == defaultValue)
        } else fixed = currentInput
        return Pair(fixed, edited)
    }

    fun arguments(expression: Boolean, message: String) {
        if (!expression) throw IllegalArgumentException(message)
    }

    fun validSnowflake(snowflake: String): Boolean {
        return try {
            MiscUtil.parseSnowflake(snowflake)
            true
        } catch (exception: Exception) {
            false
        }
    }

    fun validUrl(url: String): Boolean {
        val urlRegex = Regex(
            "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
        )
        return urlRegex.matches(url)
    }

    fun validEmail(email: String): Boolean {
        val emailRegex by lazy {
            Regex("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,8}\$")
        }

        return emailRegex.matches(email)
    }

    fun validClientKey(key: String): Boolean {
        return key.split("_")[0] == "ptlc" && key.length == 48
    }

    fun checkVersion() {
        val apiUrl = "https://api.github.com/repos/Goksi/PteroBot/releases/latest"
        val properties = Properties()
            .apply { load(this@Checks::class.java.classLoader.getResourceAsStream("version.properties")) }
        val currentVer = properties.getProperty("version")!!
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
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
                        .getString("tag_name").replace("v", "")

                    if (SemVer(fetchedVer) > SemVer(currentVer))
                        logger.warn("You are not running latest version of PteroBot ! Latest: v$fetchedVer Current: v$currentVer")
                }

            }
        })
    }
}