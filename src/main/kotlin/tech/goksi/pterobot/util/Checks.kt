package tech.goksi.pterobot.util

import net.dv8tion.jda.api.utils.MiscUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object Checks  {
    val logger: Logger = LoggerFactory.getLogger(Checks::class.java)

    fun arguments(expression: Boolean, message: String) {
        if(!expression) throw IllegalArgumentException(message)
    }

    fun validSnowflake(snowflake: String): Boolean {
        return try{
            MiscUtil.parseSnowflake(snowflake)
            true;
        }catch (exception: Exception){
            false
        }
    }

    fun validUrl(url: String): Boolean {
        val urlRegex = Regex(
            "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
        )
        return urlRegex.matches(url)
    }
}