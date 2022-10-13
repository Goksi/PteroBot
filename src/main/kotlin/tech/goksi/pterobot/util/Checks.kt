package tech.goksi.pterobot.util

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.utils.MiscUtil

object Checks  {
    val logger by SLF4J

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