package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroBuilder
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication
import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import tech.goksi.pterobot.entities.ButtonInfo
import tech.goksi.pterobot.events.handlers.PteroEventManager
import tech.goksi.pterobot.manager.ConfigManager

object Common {
    private val logger by SLF4J<PteroEventManager>()

    val ansiRegex by lazy { ">?\u001B\\[[\\d;]*[^\\d;]".toRegex() }
    private val newLineRegex by lazy { "\r?\n?".toRegex() }
    fun createClient(apiKey: String?): PteroClient? {
        if (apiKey == null) return null
        val appUrl = ConfigManager.getString("BotInfo.PterodactylUrl")
        return PteroBuilder.create(appUrl, apiKey).setEventManager(PteroEventManager()).buildClient()
    }

    private fun createApplication(apiKey: String): PteroApplication {
        val appUrl = ConfigManager.getString("BotInfo.PterodactylUrl")
        return PteroBuilder.createApplication(appUrl, apiKey)
    }

    fun getDefaultApplication(): PteroApplication {
        val app by lazy {
            createApplication(ConfigManager.getString("BotInfo.AdminApiKey"))
        }
        return app
    }

    fun String.replace(vararg replacements: Pair<String, String>): String {
        var result = this
        replacements.forEach { (first, second) -> result = result.replace(first, second) }
        return result
    }

    fun getDefaultCoroutineScope(name: String = "PteroBotScope"): CoroutineScope {
        val parentJob = SupervisorJob()
        val errorHandler = CoroutineExceptionHandler { _, throwable ->
            logger.error("Exception thrown in PteroBot coroutine !", throwable)
            if (throwable is Error) {
                parentJob.cancel()
                throw throwable
            }
        }
        return CoroutineScope(Dispatchers.IO + parentJob + errorHandler + CoroutineName(name))
    }

    suspend fun ClientServer.getLogs(): String {
        val stringBuilder = StringBuilder()
        val wssBuilder = this.webSocketBuilder
        val task = (wssBuilder.eventManager as PteroEventManager).websocketListener {
            val line = it.line.replace(newLineRegex, "")
            if (line.isEmpty()) return@websocketListener
            stringBuilder.append(line).append('\n')
        }
        val wss = wssBuilder.addEventListeners(task).build()
        delay(task.expireAfter)
        task.cancel()
        wss.shutdown()
        return stringBuilder.toString()
    }

    fun List<Button>.toActionRow(): List<ActionRow> {
        return this.chunked(4) { ActionRow.of(it) }
    }

    inline fun <reified T> createSelectMenu(
        id: String,
        placeholder: String,
        items: Collection<T>,
        format: (StringSelectMenu.Builder, T) -> Unit
    ): StringSelectMenu {
        val selectMenu = StringSelectMenu(
            customId = id,
            placeholder = ConfigManager.getString(placeholder)
        ) {
            for (item in items) format(this, item)
        }
        return selectMenu
    }

    fun button(id: String, buttonInfo: ButtonInfo) = dev.minn.jda.ktx.interactions.components.button(
        id = id,
        emoji = buttonInfo.emoji,
        style = buttonInfo.style,
        label = buttonInfo.label
    )
}
