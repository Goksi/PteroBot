package tech.goksi.pterobot.commands.handlers.creation

import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.modals.Modal
import tech.goksi.pterobot.entities.ButtonInfo
import tech.goksi.pterobot.entities.ServerCreateInfo
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.await
import tech.goksi.pterobot.util.awaitEvent
import java.util.concurrent.ThreadLocalRandom

private const val CONFIG_PATH = "Messages.Commands.Server.Create.Allocation"
private const val MODAL_ID = "pterobot:server-allocation-modal"

class AllocationButtonHandler(
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
    serverCreateInfo = serverCreateInfo,
    enabled = false
) {
    /*TODO: maybe create if not available ?*/
    override suspend fun execute(event: ButtonInteractionEvent): Boolean {
        val id = ThreadLocalRandom.current().nextInt()
        event.replyModal(createModal(id)).queue()
        val modalEvent = jda.awaitEvent<ModalInteractionEvent> { it.modalId == "$MODAL_ID:$id" } ?: return false
        val port = try {
            modalEvent.getValue("port")!!.asString.toInt()
        } catch (_: NumberFormatException) {
            modalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$CONFIG_PATH.InvalidAllocation"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return false
        }
        val allocations = serverCreateInfo.getNode()!!.retrieveAllocationsByPort(port).await()
        if (allocations.isEmpty()) {
            modalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$CONFIG_PATH.NotFound"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return false
        }
        val allocation = allocations[0]
        if (allocation.isAssigned) {
            modalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$CONFIG_PATH.InUse"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return false
        }
        serverCreateInfo.setAllocation(allocation)
        modalEvent.deferEdit().queue()
        return true
    }

    private fun createModal(id: Int): Modal {
        return Modal(
            id = "$MODAL_ID:$id",
            title = ConfigManager.getString("$CONFIG_PATH.ModalTitle")
        ) {
            short(
                id = "port",
                label = "Port",
                placeholder = ConfigManager.getString("$CONFIG_PATH.ModalPlaceholder")
            )
        }
    }
}