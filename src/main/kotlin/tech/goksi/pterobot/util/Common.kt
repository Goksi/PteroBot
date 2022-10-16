package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroBuilder
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
import tech.goksi.pterobot.manager.ConfigManager

object Common  {

    fun createClient(apiKey: String): PteroClient {
        val appUrl: String by lazy { ConfigManager.config.getString("BotInfo.PterodactylUrl") }
        return PteroBuilder.createClient(appUrl, apiKey)
    }

    fun createApplication(apiKey: String): PteroApplication {
        val appUrl: String by lazy { ConfigManager.config.getString("BotInfo.PterodactylUrl") }
        return PteroBuilder.createApplication(appUrl, apiKey)
    }
}