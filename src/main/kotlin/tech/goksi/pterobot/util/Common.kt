package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroBuilder
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
import tech.goksi.pterobot.manager.ConfigManager

object Common  {
    inline fun <T> checkInput(currentInput: T, defaultValue: T,message: String , expression: () -> T): Pair<T, Boolean> {
        var fixed: T
        var edited = false
        if(currentInput == null || currentInput == defaultValue){
            edited = true
            do{
                Checks.logger.info(message)
                fixed = expression.invoke()?:defaultValue
            } while (fixed == defaultValue)
        }else fixed = currentInput
        return Pair(fixed, edited)
    }

    fun createClient(apiKey: String): PteroClient {
        val appUrl: String by lazy { ConfigManager.config.getString("BotInfo.PterodactylUrl") }
        return PteroBuilder.createClient(appUrl, apiKey)
    }
}