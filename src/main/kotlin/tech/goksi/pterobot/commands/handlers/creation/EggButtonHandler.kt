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
import tech.goksi.pterobot.util.Common.createSelectMenu
import tech.goksi.pterobot.util.await
import tech.goksi.pterobot.util.awaitEvent
import java.util.concurrent.ThreadLocalRandom

private const val CONFIG_PATH = "Messages.Commands.Server.Create.Egg"
private const val NEST_SELECTION_ID = "pterobot:nest-selector"
private const val EGG_SELECTION_ID = "pterobot:egg-selector"
private const val IMAGE_SELECTION_ID = "pterobot:image-selector"

class EggButtonHandler(
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
        val nests = Common.getDefaultApplication().retrieveNests().await()
        val nestMenu = createSelectMenu(
            id = "$NEST_SELECTION_ID:$id",
            placeholder = ConfigManager.getString("$CONFIG_PATH.NestPlaceholder"),
            nests
        ) { builder, nest ->
            builder.option(nest.name, nest.id)
        }
        event.reply("").setActionRow(nestMenu).setEphemeral(true).queue()
        val nestSelectEvent =
            jda.awaitEvent<StringSelectInteractionEvent> { it.componentId == "$NEST_SELECTION_ID:$id" } ?: return false
        val nest = nests.first { it.id == nestSelectEvent.selectedOptions[0].value }
        val eggs = nest.retrieveEggs().await()
        val eggsMenu = createSelectMenu(
            id = "$EGG_SELECTION_ID:$id",
            placeholder = ConfigManager.getString("$CONFIG_PATH.EggPlaceholder"),
            eggs
        ) { builder, egg ->
            builder.option(egg.name, egg.id, "Author: ${egg.author}")
        }
        nestSelectEvent.reply("").setActionRow(eggsMenu).setEphemeral(true).queue()
        event.hook.deleteOriginal().queue()
        val eggSelectEvent =
            jda.awaitEvent<StringSelectInteractionEvent> { it.componentId == "$EGG_SELECTION_ID:$id" } ?: return false
        val egg = eggs.first { it.id == eggSelectEvent.selectedOptions[0].value }
        val dockerImages = egg.dockerImages
        nestSelectEvent.hook.deleteOriginal().queue()
        if (dockerImages.size > 1) {
            val dockerImageMenu = createSelectMenu(
                id = "$IMAGE_SELECTION_ID:$id",
                placeholder = ConfigManager.getString("$CONFIG_PATH.ImagePlaceholder"),
                dockerImages
            ) { builder, dockerImage ->
                builder.option(
                    dockerImage.name + if (dockerImage.image == egg.dockerImage) " (Default)" else "",
                    dockerImage.image,
                    dockerImage.image
                )
            }
            eggSelectEvent.reply("").setEphemeral(true).setActionRow(dockerImageMenu).queue()
            val dockerImageSelectEvent =
                jda.awaitEvent<StringSelectInteractionEvent> { it.componentId == "$IMAGE_SELECTION_ID:$id" }
                    ?: return false
            val dockerImage = dockerImages.first { it.image == dockerImageSelectEvent.selectedOptions[0].value }
            serverCreateInfo.dockerImage = dockerImage.image
            dockerImageSelectEvent.deferEdit().queue()
            eggSelectEvent.hook.deleteOriginal().queue()
        }
        if (!eggSelectEvent.isAcknowledged) eggSelectEvent.deferEdit().queue()
        serverCreateInfo.setEgg(egg)
        return true
    }
}