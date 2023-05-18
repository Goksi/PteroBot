package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.PowerAction
import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.exceptions.LoginException
import com.mattmalec.pterodactyl4j.exceptions.ServerException
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onButton
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload
import okhttp3.internal.toLongOrDefault
import tech.goksi.pterobot.commands.manager.abs.SimpleSubcommand
import tech.goksi.pterobot.commands.manager.abs.TopLevelCommand
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.entities.ServerCreate
import tech.goksi.pterobot.entities.ServerInfo
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.Common.getLogs
import tech.goksi.pterobot.util.await
import tech.goksi.pterobot.util.awaitEvent
import tech.goksi.pterobot.util.cooldown.CooldownManager.cooldownButton
import tech.goksi.pterobot.util.cooldown.CooldownType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom
import kotlin.time.Duration.Companion.minutes

private const val SERVER_PATH = "Messages.Commands.Server"

class ServerCommand(jda: JDA) : TopLevelCommand(
    name = "server",
    subcommands = listOf(List(jda), Create(jda))
)

private class List(jda: JDA) : SimpleSubcommand(
    name = "list",
    description = ConfigManager.getString("$SERVER_PATH.List.Description"),
    baseCommand = "server"
) {
    private val logger by SLF4J
    private val serverMapping: MutableMap<String, ClientServer> = HashMap()

    init {
        jda.listener<ModalInteractionEvent>(timeout = 2.minutes) {
            val id = it.modalId
            if (id.startsWith("pterobot:command")) {
                val server = serverMapping[id.split(":")[2]]!!.also { serverMapping.remove(id.split(":")[2]) }
                server.sendCommand(it.getValue("command")!!.asString).executeAsync({ _ ->
                    it.replyEmbeds(
                        EmbedManager.getGenericSuccess(ConfigManager.getString("$SERVER_PATH.List.SuccessCommand"))
                            .toEmbed()
                    ).setEphemeral(true).queue()
                }) { throwable ->
                    it.replyEmbeds(
                        EmbedManager.getGenericFailure(ConfigManager.getString("Embeds.UnexpectedError"))
                            .toEmbed()
                    ).setEphemeral(true).queue()
                    logger.error("Error while sending command to ${server.name}", throwable)
                }
            }
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(ConfigManager.getBoolean("BotInfo.Ephemeral")).queue()
        val pteroMember = PteroMember(event.user)
        if (pteroMember.isLinked()) {
            val servers = try {
                pteroMember.getServers()
            } catch (exception: LoginException) {
                event.hook.sendMessageEmbeds(
                    EmbedManager
                        .getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.NotFound"))
                        .toEmbed()
                ).queue()
                return
            }
            val selectMenu = StringSelectMenu("pterobot:servers-selector:${event.user.idLong}") {
                for (server in servers) {
                    this.option(label = server.name, value = server.identifier)
                }
                this.placeholder = ConfigManager.getString("$SERVER_PATH.List.MenuPlaceholder")
            }
            val response = EmbedManager.getServersCommand().toEmbed()

            event.hook.sendMessageEmbeds(response).addActionRow(selectMenu).queue()
        } else {
            event.hook.sendMessageEmbeds(
                EmbedManager
                    .getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.NotLinked")).toEmbed()
            ).queue()
        }
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith("pterobot:servers-selector:")) return
        if (event.componentId.split(":")[2] != event.user.id) {
            event.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.WrongUser"))
                    .toEmbed()
            )
                .setEphemeral(true).queue()
            return
        }
        event.deferReply(ConfigManager.getBoolean("BotInfo.Ephemeral")).queue()
        val pteroMember = PteroMember(event.user)
        event.message.delete().queue()
        if (!pteroMember.isLinked()) {
            event.hook.sendMessageEmbeds(
                EmbedManager
                    .getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.NotLinked")).toEmbed()
            ).setEphemeral(true).queue()
            return
        }
        val server = try {
            pteroMember.getServerById(event.selectedOptions[0].value)
        } catch (exception: LoginException) {
            event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.WrongKey"))
                    .toEmbed()
            ).queue()
            return
        }
        val serverInfo = try {
            ServerInfo(server)
        } catch (exception: ServerException) {
            event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.NodeOffline"))
                    .toEmbed()
            ).queue()
            return
        }
        val response = EmbedManager.getServerInfo(serverInfo).toEmbed()
        val buttons = getButtons(server, serverInfo, event)
        event.hook.sendMessageEmbeds(response).addActionRow(buttons.subList(0, 5))
            .addActionRow(buttons.subList(5, buttons.size)).queue()
    }

    private fun getButtonSetting(setting: String) = ConfigManager.getString("$SERVER_PATH.List.Buttons.$setting")

    private fun getButtons(
        server: ClientServer,
        serverInfo: ServerInfo,
        event: StringSelectInteractionEvent
    ): kotlin.collections.List<Button> {
        /*START OR STOP BTN*/
        val changeStateButton = when (serverInfo.status) {
            "RUNNING", "STARTING" -> event.jda.cooldownButton(
                style = ButtonStyle.valueOf(getButtonSetting("StopType")),
                user = event.user,
                label = getButtonSetting("Stop"),
                emoji = Emoji.fromUnicode(getButtonSetting("StopEmoji")),
                type = CooldownType.STATUS_BTN
            ) { buttonEvent ->
                server.setPower(PowerAction.STOP).executeAsync({
                    buttonEvent.hook.sendMessageEmbeds(
                        EmbedManager.getGenericSuccess(
                            ConfigManager.getString(
                                "$SERVER_PATH.List.SuccessStop"
                            )
                        ).toEmbed()
                    )
                        .setEphemeral(true).queue()
                }) {
                    buttonEvent.hook.sendMessageEmbeds(
                        EmbedManager.getGenericFailure(ConfigManager.getString("Embeds.UnexpectedError"))
                            .toEmbed()
                    )
                        .setEphemeral(true).queue().also { _ ->
                            logger.error("Error while changing server state !", it)
                        }
                }
            }

            else -> event.jda.cooldownButton(
                style = ButtonStyle.valueOf(getButtonSetting("StartType")),
                user = event.user,
                disabled = serverInfo.status == "UNKNOWN",
                label = getButtonSetting("Start"),
                emoji = Emoji.fromUnicode(getButtonSetting("StartEmoji")),
                type = CooldownType.STATUS_BTN
            ) { buttonEvent ->
                server.setPower(PowerAction.START).executeAsync({
                    buttonEvent.hook.sendMessageEmbeds(
                        EmbedManager.getGenericSuccess(
                            ConfigManager.getString(
                                "$SERVER_PATH.List.SuccessStart"
                            )
                        ).toEmbed()
                    )
                        .setEphemeral(true).queue()
                }) {
                    buttonEvent.hook.sendMessageEmbeds(
                        EmbedManager.getGenericFailure(ConfigManager.getString("Messages.Embeds.UnexpectedError"))
                            .toEmbed()
                    )
                        .setEphemeral(true).queue().also { _ ->
                            logger.error("Error while changing server state !", it)
                        }
                }
            }
        }
        /*RESTART BTN*/
        val restartButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("RestartType")),
            user = event.user,
            disabled = serverInfo.status == "UNKNOWN",
            label = getButtonSetting("Restart"),
            emoji = Emoji.fromUnicode(getButtonSetting("RestartEmoji")),
            type = CooldownType.RESTART_BTN
        ) { buttonEvent ->
            server.setPower(PowerAction.RESTART).executeAsync({
                buttonEvent.hook.sendMessageEmbeds(
                    EmbedManager.getGenericSuccess(
                        ConfigManager.getString(
                            "$SERVER_PATH.List.SuccessRestart"
                        )
                    ).toEmbed()
                )
                    .setEphemeral(true).queue()
            }) {
                buttonEvent.hook.sendMessageEmbeds(
                    EmbedManager.getGenericFailure(ConfigManager.getString("Embeds.UnexpectedError"))
                        .toEmbed()
                )
                    .setEphemeral(true).queue().also { _ ->
                        logger.error("Error while changing server state !", it)
                    }
            }
        }
        /*COMMAND BTN*/
        val commandButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("CommandType")),
            user = event.user,
            disabled = serverInfo.status != "RUNNING",
            label = getButtonSetting("Command"),
            emoji = Emoji.fromUnicode(getButtonSetting("CommandEmoji")),
            type = CooldownType.COMMAND_BTN
        ) { buttonEvent ->
            val commandModal = Modal(
                id = "pterobot:command:${server.identifier}",
                title = ConfigManager.getString("$SERVER_PATH.List.Modal.Name")
            ) {
                this.short(
                    id = "command",
                    label = "Command",
                    required = true,
                    placeholder = ConfigManager.getString("$SERVER_PATH.List.Modal.Placeholder")
                )
            }
            buttonEvent.replyModal(commandModal).queue()
            serverMapping[server.identifier] = server
        }
        /*REQUEST LOGS BTN*/
        val requestLogsButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("RequestLogsType")),
            user = event.user,
            disabled = serverInfo.status != "RUNNING",
            label = getButtonSetting("RequestLogs"),
            emoji = Emoji.fromUnicode(getButtonSetting("RequestLogsEmoji")),
            type = CooldownType.LOGS_BTN
        ) {
            it.deferReply(true).queue()
            it.hook.sendFiles(
                FileUpload.fromData(
                    server.getLogs().replace(Common.ansiRegex, "").byteInputStream(),
                    "${server.name}-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyy_HH-mm"))}.txt"
                )
            ).queue()
        }
        /*CLOSE BTN*/
        val closeButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("CloseType")),
            user = event.user,
            label = getButtonSetting("Close"),
            emoji = Emoji.fromUnicode(getButtonSetting("CloseEmoji"))
        ) {
            it.deferEdit().queue()
            it.hook.deleteOriginal().queue()
        }
        val refreshButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("RefreshType")),
            user = event.user,
            label = getButtonSetting("Refresh"),
            emoji = Emoji.fromUnicode(getButtonSetting("RefreshEmoji")),
            type = CooldownType.REFRESH_BTN
        ) {
            val serverNew = server.refreshData().await()
            val serverInfoNew = try {
                ServerInfo(serverNew)
            } catch (exception: ServerException) {
                it.editMessageEmbeds(
                    EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.List.NodeOffline"))
                        .toEmbed()
                ).setActionRow(closeButton).queue()
                return@cooldownButton
            }
            val newButtons = getButtons(serverNew, serverInfoNew, event)
            val rows =
                listOf(ActionRow.of(newButtons.subList(0, 5)), ActionRow.of(newButtons.subList(5, newButtons.size)))
            it.editMessageEmbeds(EmbedManager.getServerInfo(serverInfoNew).toEmbed()).setReplace(true)
                .setComponents(rows).queue()
        }

        return listOf(changeStateButton, restartButton, commandButton, requestLogsButton, refreshButton, closeButton)
    }
}

private class Create(val jda: JDA) : SimpleSubcommand(
    name = "create",
    description = ConfigManager.getString("$SERVER_PATH.Create.Description"),
    baseCommand = "server"
) {
    val closeButton = button(
        id = "pterobot:server-create:close",
        style = ButtonStyle.valueOf(getButtonSetting("CloseType")),
        label = getButtonSetting("Close"),
        emoji = Emoji.fromUnicode(getButtonSetting("CloseEmoji"))
    )

    init {
        jda.onButton("pterobot:server-create:close") {
            it.hook.deleteOriginal().queue()
            it.deferEdit().queue()
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val pteroMember = PteroMember(event.user.idLong)
        if (!pteroMember.isPteroAdmin()) {
            event.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.Create.NotAdmin"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return
        }
        val randomId = ThreadLocalRandom.current().nextInt()
        val serverCreation = ServerCreate()
        val serverInfoButton = event.jda.button(
            style = ButtonStyle.valueOf(getButtonSetting("ServerInfoType")),
            label = getButtonSetting("ServerInfo"),
            emoji = Emoji.fromUnicode(getButtonSetting("ServerInfoEmoji"))
        ) { buttonEvent ->
            val serverInfoModal = Modal(
                id = "pterobot:server-info-modal:${event.user.idLong}:$randomId",
                title = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoModalTitle")
            ) {
                short(
                    id = "name",
                    label = "Server name",
                    required = true,
                    placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoNamePlaceholder"),
                    value = if (serverCreation.serverName == ServerCreate.NOT_SET) null else serverCreation.serverName
                )
                paragraph(
                    id = "desc",
                    label = "Description",
                    required = false,
                    placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoDescriptionPlaceholder"),
                    value = if (serverCreation.serverDescription == ServerCreate.NOT_SET) null else serverCreation.serverDescription
                )
                short(
                    id = "memory",
                    label = "Memory",
                    required = true,
                    placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoMemoryPlaceholder"),
                    value = if (serverCreation.memory == -1L) null else serverCreation.memory.toString()
                )
                short(
                    id = "disk",
                    label = "Disk space",
                    required = true,
                    placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoDiskPlaceholder"),
                    value = if (serverCreation.disk == -1L) null else serverCreation.disk.toString()
                )
            }
            buttonEvent.replyModal(serverInfoModal).queue()
            val serverInfoModalEvent =
                buttonEvent.jda.awaitEvent<ModalInteractionEvent>() { it.modalId == "pterobot:server-info-modal:${event.user.idLong}:$randomId" }
                    ?: return@button
            val name = serverInfoModalEvent.getValue("name")!!.asString
            serverCreation.serverName = name
            val descTemp = serverInfoModalEvent.getValue("desc")!!.asString
            val description = if (descTemp == "") ServerCreate.NOT_SET else descTemp
            serverCreation.serverDescription = description
            /*TODO check >= 0 and return message*/
            val memory = serverInfoModalEvent.getValue("memory")!!.asString.toLongOrDefault(-1)
            if (memory < 0) {
                buttonEvent.replyEmbeds(
                    EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.Create.InvalidMemory"))
                        .toEmbed()
                ).queue()
                return@button
            }
            serverCreation.memory = memory
            val disk = serverInfoModalEvent.getValue("disk")!!.asString.toLongOrDefault(-1)
            if (disk < 0) {
                buttonEvent.replyEmbeds(
                    EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.Create.InvalidDisk"))
                        .toEmbed()
                ).queue()
                return@button
            }
            serverCreation.disk = disk
            serverInfoModalEvent.deferEdit().queue()

            buttonEvent.hook.editOriginalEmbeds(EmbedManager.getServerCreate(serverCreation).toEmbed()).queue()
        }

        event.replyEmbeds(EmbedManager.getServerCreate(serverCreation).toEmbed())
            .setActionRow(serverInfoButton, closeButton)
            .setEphemeral(true).queue()
    }
    /*override suspend fun execute(event: SlashCommandInteractionEvent) {
        val pteroMember = PteroMember(event.user.idLong)

        if (!pteroMember.isPteroAdmin()) {
            event.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.Create.NotAdmin"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return
        }
        val pteroApplication = Common.getDefaultApplication()
        val nodes = pteroApplication.retrieveNodes().await()
        val randomId = ThreadLocalRandom.current().nextInt()
        *//*Send node select menu*//*
        val nodeSelectMenu = createSelectMenu(
            "pterobot:node-selector:${event.user.idLong}:$randomId",
            ConfigManager.getString("$SERVER_PATH.Create.NodeMenuPlaceholder"),
            nodes
        ) { builder, node ->
            builder.option(
                node.name,
                node.id,
                "Max memory: ${node.memory}MB Allocated memory: ${node.allocatedMemory}MB"
            )
        }
        event.reply("")
            .setActionRow(nodeSelectMenu)
            .setEphemeral(true).queue()
        val selectNodeEvent =
            jda.awaitEvent<StringSelectInteractionEvent> { it.componentId == "pterobot:node-selector:${event.user.idLong}:$randomId" }
                ?: return
        val selectedNode = nodes.first { it.id == selectNodeEvent.selectedOptions[0].value }
        *//*Send allocation modal*//*
        val allocationModal = Modal(
            id = "pterobot:allocation-modal:${event.user.idLong}:$randomId",
            title = ConfigManager.getString("$SERVER_PATH.Create.AllocationModalTitle")
        ) {
            short(
                id = "allocation",
                label = "Port",
                required = true,
                placeholder = ConfigManager.getString("$SERVER_PATH.Create.AllocationModalPlaceholder"),
                requiredLength = 4..5
            )
        }
        selectNodeEvent.replyModal(allocationModal).queue()
        selectNodeEvent.hook.deleteOriginal().queue()
        val allocationModalEvent =
            jda.awaitEvent<ModalInteractionEvent> { it.modalId == "pterobot:allocation-modal:${event.user.idLong}:$randomId" }
                ?: return
        *//*Parse port and get allocation object*//*
        val port = allocationModalEvent.getValue("allocation")!!.asString.toIntOrNull() ?: 0
        val tempAllocation = selectedNode.retrieveAllocationsByPort(port).await()
        *//*TODO: check multiple ips matching same port ?*//*
        if (tempAllocation.isEmpty()) {
            allocationModalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.Create.AllocationNotFound"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return
        }
        val allocation = tempAllocation[0]
        *//*Send egg select menu*//*
        val eggs = pteroApplication.retrieveEggs().await()
        val eggSelectMenu = createSelectMenu(
            "pterobot:egg-selector:${event.user.idLong}:$randomId",
            ConfigManager.getString("$SERVER_PATH.Create.EggMenuPlaceholder"),
            eggs
        ) { builder, egg ->
            builder.option(
                egg.name,
                "${egg.retrieveNest().await().id}:${egg.id}",
                egg.dockerImage
            )
        }
        allocationModalEvent.reply("")
            .setActionRow(eggSelectMenu)
            .setEphemeral(true).queue()
        val selectEggEvent =
            jda.awaitEvent<StringSelectInteractionEvent> { it.componentId == "pterobot:egg-selector:${event.user.idLong}:$randomId" }
                ?: return
        val (nestId, eggId) = selectEggEvent.selectedOptions[0].value.split(":")
        val egg = pteroApplication.retrieveEggById(nestId, eggId).await()
        selectEggEvent.hook.deleteOriginal().queue()
        *//*Send owner modal*//*
        val ownerModal = Modal(
            id = "pterobot:owner-modal:${event.user.idLong}:$randomId",
            title = ConfigManager.getString("$SERVER_PATH.Create.OwnerModalTitle")
        ) {
            short(
                id = "owner",
                label = "Email",
                required = true,
                placeholder = ConfigManager.getString("$SERVER_PATH.Create.OwnerModalPlaceholder")
            )
        }
        selectEggEvent.replyModal(ownerModal).queue()
        val selectOwnerEvent =
            jda.awaitEvent<ModalInteractionEvent> { it.modalId == "pterobot:owner-modal:${event.user.idLong}:$randomId" }
                ?: return
        val tempOwner =
            pteroApplication.retrieveUsersByEmail(selectOwnerEvent.getValue("owner")!!.asString, false).await()
        if (tempAllocation.isEmpty()) {
            allocationModalEvent.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.getString("$SERVER_PATH.Create.OwnerNotFound"))
                    .toEmbed()
            ).setEphemeral(true).queue()
            return
        }
        val owner = tempOwner[0]
        *//*Send basic server info modal*//*
        val serverInfoModal = Modal(
            id = "pterobot:server-info-modal:${event.user.idLong}:$randomId",
            title = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoModalTitle")
        ) {
            short(
                id = "name",
                label = "Server name",
                required = true,
                placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoNamePlaceholder")
            )
            paragraph(
                id = "desc",
                label = "Description",
                placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoDescriptionPlaceholder")
            )
            short(
                id = "memory",
                label = "Memory",
                required = true,
                placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoMemoryPlaceholder")
            )
            short(
                id = "disk",
                label = "Disk space",
                required = true,
                placeholder = ConfigManager.getString("$SERVER_PATH.Create.ServerInfoDiskPlaceholder")
            )
        } // smh cant reply modal from modal event

        val createServerAction = pteroApplication.createServer()
            .startOnCompletion(true)
            .setEgg(egg)
            .setAllocation(allocation)
            .setOwner(owner)

    }*/

    private inline fun <reified T> createSelectMenu(
        id: String,
        placeholder: String,
        items: kotlin.collections.List<T>,
        option: (StringSelectMenu.Builder, T) -> Unit
    ): StringSelectMenu {
        val selectMenu = StringSelectMenu(customId = id, placeholder = placeholder) {
            for (item in items) option(this, item)
        }
        return selectMenu
    }

    private fun getButtonSetting(setting: String) =
        ConfigManager.getString("$SERVER_PATH.Create.Buttons.$setting")
}
