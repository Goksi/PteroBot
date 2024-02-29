package tech.goksi.pterobot.commands.handlers.creation

import dev.minn.jda.ktx.interactions.components.option
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import tech.goksi.pterobot.entities.ButtonInfo
import tech.goksi.pterobot.entities.ServerCreateInfo
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.await
import tech.goksi.pterobot.util.awaitEvent
import java.util.concurrent.ThreadLocalRandom

private const val CONFIG_PATH = "Messages.Commands.Server.Create.Node"
private const val SELECTION_ID = "pterobot:node-selector"

class NodeButtonHandler(
    jda: JDA,
    buttonInfo: ButtonInfo,
    user: User,
    createServerId: String,
    serverCreateInfo: ServerCreateInfo,
    allocationButtonId: String
) : ServerCreationButtonHandler(
    jda = jda,
    buttonInfo = buttonInfo,
    user = user,
    createServerId = createServerId,
    serverCreateInfo = serverCreateInfo,
    activate = allocationButtonId
) {
    override suspend fun execute(event: ButtonInteractionEvent): Boolean {
        val id = ThreadLocalRandom.current().nextInt()
        val nodes = Common.getDefaultApplication().retrieveNodes().await()
        val selectMenu = Common.createSelectMenu(
            "$SELECTION_ID:$id",
            ConfigManager.getString("$CONFIG_PATH.MenuPlaceholder"),
            nodes
        ) { builder, node ->
            builder.option(
                node.name,
                node.id,
                "Max memory: ${node.memory}MB Allocated memory: ${node.allocatedMemory}MB"
            )
        }
        event.reply("").setActionRow(selectMenu).setEphemeral(true).queue()
        val selectEvent =
            jda.awaitEvent<StringSelectInteractionEvent> { it.componentId == "$SELECTION_ID:$id" } ?: return false
        val node = nodes.first { it.id == selectEvent.selectedOptions[0].value }
        if (serverCreateInfo.node != node.name) {
            serverCreateInfo.removeAllocation()
            serverCreateInfo.setNode(node)
        } else return false
        selectEvent.deferEdit().queue()
        selectEvent.hook.deleteOriginal().queue()
        return true
    }
}