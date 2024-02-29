package tech.goksi.pterobot.commands.handlers.creation

import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.modals.Modal
import okhttp3.internal.toLongOrDefault
import tech.goksi.pterobot.entities.ButtonInfo
import tech.goksi.pterobot.entities.ServerCreateInfo
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.awaitEvent
import java.util.concurrent.ThreadLocalRandom

private const val CONFIG_PATH = "Messages.Commands.Server.Create.ServerInfo"
private const val MODAL_ID = "pterobot:server-info-modal"

class ServerInfoButtonHandler(
    jda: JDA,
    buttonInfo: ButtonInfo,
    user: User,
    createServerId: String,
    serverCreateInfo: ServerCreateInfo
) : ServerCreationButtonHandler(
    jda = jda,
    buttonInfo = buttonInfo,
    user = user,
    createServerId = createServerId,
    serverCreateInfo = serverCreateInfo
) {
    override suspend fun execute(event: ButtonInteractionEvent): Boolean {
        val id = ThreadLocalRandom.current().nextInt()
        event.replyModal(createModal(id)).queue()
        val modalEvent = jda.awaitEvent<ModalInteractionEvent> { it.modalId == "$MODAL_ID:$id" } ?: return false
        val serverName = modalEvent.getValue("name")!!.asString
        serverCreateInfo.serverName = serverName
        val serverDescription = modalEvent.getValue("desc")!!.asString
            .takeIf { it.isNotBlank() } ?: ServerCreateInfo.NOT_SET
        serverCreateInfo.serverDescription = serverDescription
        val memory = modalEvent.getValue("memory")!!.asString.toLongOrDefault(-1)
        if (memory <= 0) {
            modalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$CONFIG_PATH.InvalidMemory"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return false
        }
        serverCreateInfo.memory = memory
        val disk = modalEvent.getValue("disk")!!.asString.toLongOrDefault(-1)
        if (disk <= 0) {
            modalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$CONFIG_PATH.InvalidDisk"))
                    .toEmbed()
            ).setEphemeral(true).queue()
        }
        serverCreateInfo.disk = disk
        modalEvent.deferEdit().queue()
        return true
    }

    private fun createModal(id: Int): Modal {
        return Modal(
            id = "$MODAL_ID:$id",
            title = ConfigManager.getString("$CONFIG_PATH.ModalTitle")
        ) {
            short(
                id = "name",
                label = "Server name",
                required = true,
                placeholder = ConfigManager.getString("$CONFIG_PATH.NamePlaceholder"),
                value = if (serverCreateInfo.serverName == ServerCreateInfo.NOT_SET) null else serverCreateInfo.serverName
            )
            paragraph(
                id = "desc",
                label = "Description",
                required = false,
                placeholder = ConfigManager.getString("$CONFIG_PATH.DescriptionPlaceholder"),
                value = if (serverCreateInfo.serverDescription == ServerCreateInfo.NOT_SET) null else serverCreateInfo.serverDescription
            )
            short(
                id = "memory",
                label = "Memory",
                required = true,
                placeholder = ConfigManager.getString("$CONFIG_PATH.MemoryPlaceholder"),
                value = if (serverCreateInfo.memory == -1L) null else serverCreateInfo.memory.toString()
            )
            short(
                id = "disk",
                label = "Disk space",
                required = true,
                placeholder = ConfigManager.getString("$CONFIG_PATH.DiskPlaceholder"),
                value = if (serverCreateInfo.disk == -1L) null else serverCreateInfo.disk.toString()
            )
        }
    }
}