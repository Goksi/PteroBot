package tech.goksi.pterobot.util

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.utils.MiscUtil

object Checks  {
    val logger by SLF4J
    inline fun <T> checkInput(currentInput: T, defaultValue: T,message: String , expression: () -> T): Pair<T, Boolean> {
        var fixed: T
        var edited = false
        if(currentInput == null || currentInput == defaultValue){
            edited = true
            do{
                logger.info(message)
                fixed = expression.invoke()?:defaultValue
            } while (fixed == defaultValue)
        }else fixed = currentInput
        return Pair(fixed, edited)
    }

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