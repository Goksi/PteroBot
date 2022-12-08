package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroBuilder
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication
import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import tech.goksi.pterobot.events.handlers.PteroEventManager
import tech.goksi.pterobot.manager.ConfigManager
import java.lang.StringBuilder

object Common {
    private val logger by SLF4J<PteroEventManager>()

    val ansiRegex by lazy { ">?\u001B\\[[\\d;]*[^\\d;]".toRegex() }
    fun createClient(apiKey: String?): PteroClient? {
        if (apiKey == null) return null
        val appUrl = ConfigManager.config.getString("BotInfo.PterodactylUrl")
        return PteroBuilder.create(appUrl, apiKey).setEventManager(PteroEventManager()).buildClient()
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

    fun String.replace(vararg replacements: Pair<String, String>): String {
        var result = this
        replacements.forEach { (first, second) -> result = result.replace(first, second) }
        return result
    }

    fun getDefaultCoroutineScope(): CoroutineScope {
        val parentJob = SupervisorJob()
        val errorHandler = CoroutineExceptionHandler { _, throwable ->
            logger.error("Exception thrown in pterodactyl coroutine !", throwable)
            if (throwable is Error) {
                parentJob.cancel()
                throw throwable
            }
        }
        return CoroutineScope(Dispatchers.IO + parentJob + errorHandler)
    }

    suspend fun ClientServer.getLogs(): String {
        val stringBuilder = StringBuilder()
        val wssBuilder = this.webSocketBuilder
        val task = (wssBuilder.eventManager as PteroEventManager).websocketListener {
            val line = it.line
            if (line.isEmpty()) return@websocketListener
            if (!line.contains('\r') && stringBuilder.isNotEmpty()) stringBuilder.append('\n')
            stringBuilder.append(line)
        }
        val wss = wssBuilder.addEventListeners(task).build()
        delay(task.expireAfter)
        task.cancel()
        wss.shutdown()
        return stringBuilder.toString()
    }
}
