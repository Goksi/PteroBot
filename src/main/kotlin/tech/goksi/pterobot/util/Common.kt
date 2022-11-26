package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroBuilder
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
import tech.goksi.pterobot.manager.ConfigManager

object Common {

    fun createClient(apiKey: String?): PteroClient? {
        if (apiKey == null) return null
        val appUrl = ConfigManager.config.getString("BotInfo.PterodactylUrl")
        return PteroBuilder.createClient(appUrl, apiKey)
    }

    private fun createApplication(apiKey: String): PteroApplication {
        val appUrl = ConfigManager.config.getString("BotInfo.PterodactylUrl")
        return PteroBuilder.createApplication(appUrl, apiKey)
    }

    fun getDefaultApplication(): PteroApplication {
        val app by lazy {
            createApplication(ConfigManager.config.getString("BotInfo.AdminApiKey"))
        }
        return app
    }
}
