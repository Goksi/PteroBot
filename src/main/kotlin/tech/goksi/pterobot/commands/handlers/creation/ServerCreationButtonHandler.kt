package tech.goksi.pterobot.commands.handlers.creation

import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import tech.goksi.pterobot.commands.handlers.ButtonHandler
import tech.goksi.pterobot.entities.ButtonInfo
import tech.goksi.pterobot.entities.ServerCreateInfo
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common.toActionRow

abstract class ServerCreationButtonHandler(
    jda: JDA,
    buttonInfo: ButtonInfo,
    user: User? = null,
    enabled: Boolean = true,
    private val activate: String? = null,
    private val createServerId: String,
    protected val serverCreateInfo: ServerCreateInfo
) : ButtonHandler(jda, buttonInfo, user, enabled) {
    abstract suspend fun execute(event: ButtonInteractionEvent): Boolean

    override suspend fun handle(event: ButtonInteractionEvent) {
        val success = execute(event)
        if (!success) return
        val buttons = event.message.buttons
        val modifiedButtons = buttons.map {
            if (it.id!! == (
                        activate
                            ?: "none"
                        ) || (createServerId == it.id && serverCreateInfo.canCreate())
            ) return@map it.asEnabled()
            else return@map it
        }
        val editedMessage = MessageEdit(
            embeds = listOf(EmbedManager.getServerCreate(serverCreateInfo).toEmbed()),
            components = modifiedButtons.toActionRow()
        )
        event.message.editMessage(editedMessage).queue()
    }
}