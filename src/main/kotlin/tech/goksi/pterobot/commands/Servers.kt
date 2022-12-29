package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.PowerAction
import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.exceptions.LoginException
import com.mattmalec.pterodactyl4j.exceptions.ServerException
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageEditBuilder
import dev.minn.jda.ktx.messages.editMessage_
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.FileUpload
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.entities.AccountInfo
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.entities.ServerInfo
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.Common.getLogs
import tech.goksi.pterobot.util.cooldown.CooldownManager.cooldownButton
import tech.goksi.pterobot.util.cooldown.CooldownType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

private const val CONFIG_PREFIX = "Messages.Commands.Servers."
private const val SELECTION_ID = "pterobot:servers-selector"
/*TODO single event for every close btn*/
class Servers(jda: JDA) : SimpleCommand() {
    private val logger by SLF4J
    private val serverMapping: MutableMap<String, ClientServer> = HashMap()

    init {
        this.name = "servers"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
    }

    /*LISTENER FOR COMMAND*/
    init {
        jda.listener<ModalInteractionEvent>(timeout = 2.minutes) {
            val id = it.modalId
            if (id.startsWith("pterobot:command")) {
                val server = serverMapping[id.split(":")[2]]!!.also { serverMapping.remove(id.split(":")[2]) }
                server.sendCommand(it.getValue("command")!!.asString).executeAsync({ _ ->
                    it.replyEmbeds(
                        EmbedManager.getGenericSuccess(ConfigManager.config.getString(CONFIG_PREFIX + "SuccessCommand"))
                            .toEmbed(it.jda)
                    ).setEphemeral(true).queue()
                }) { throwable ->
                    it.replyEmbeds(
                        EmbedManager.getGenericFailure(ConfigManager.config.getString("Embeds.UnexpectedError"))
                            .toEmbed(it.jda)
                    ).setEphemeral(true).queue()
                    logger.error("Error while sending command to ${server.name}", throwable)
                }
            }
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        val pteroMember = PteroMember(event.user)
        if (pteroMember.isLinked()) {
            val servers = try {
                pteroMember.getServers()
            } catch (exception: LoginException) {
                event.hook.sendMessageEmbeds(
                    EmbedManager
                        .getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotFound"))
                        .toEmbed(event.jda)
                ).queue()
                return
            }
            val selectMenu = StringSelectMenu(SELECTION_ID + ":${event.user.idLong}") {
                for (server in servers) {
                    this.option(label = server.name, value = server.identifier)
                }
                this.placeholder = ConfigManager.config.getString(CONFIG_PREFIX + "MenuPlaceholder")
            }
            val response = EmbedManager.getServersCommand(AccountInfo(pteroMember.getAccount())).toEmbed(event.jda)

            event.hook.sendMessageEmbeds(response).addActionRow(selectMenu).queue()
        } else {
            event.hook.sendMessageEmbeds(
                EmbedManager
                    .getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotLinked")).toEmbed(event.jda)
            ).queue()
        }
    }

    /*TODO: refresh button*/
    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith(SELECTION_ID)) return
        if (event.componentId.split(":")[2] != event.user.id) {
            event.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "WrongUser"))
                    .toEmbed(event.jda)
            )
                .setEphemeral(true).queue()
            return
        }
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        val pteroMember = PteroMember(event.user)
        event.message.delete().queue()
        if (!pteroMember.isLinked()) {
            event.hook.sendMessageEmbeds(
                EmbedManager
                    .getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotLinked")).toEmbed(event.jda)
            ).setEphemeral(true).queue()
            return
        }
        val server = try {
            pteroMember.getServerById(event.selectedOptions[0].value)
        } catch (exception: LoginException) {
            event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "WrongKey"))
                    .toEmbed(event.jda)
            ).queue()
            return
        }
        val serverInfo = try {
            ServerInfo(server)
        } catch (exception: ServerException) {
            event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NodeOffline"))
                    .toEmbed(event.jda)
            ).queue()
            return
        }
        val response = EmbedManager.getServerInfo(serverInfo).toEmbed(event.jda)
        val buttons = getButtons(server, serverInfo, event);
        event.hook.sendMessageEmbeds(response).addActionRow(buttons.subList(0, 5))
            .addActionRow(buttons.subList(5, buttons.size)).queue()
    }

    private fun getButtonSetting(setting: String) = ConfigManager.config.getString(CONFIG_PREFIX + "Buttons.$setting")

    private fun getButtons(
        server: ClientServer,
        serverInfo: ServerInfo,
        event: StringSelectInteractionEvent
    ): List<Button> {
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
                            ConfigManager.config.getString(
                                CONFIG_PREFIX + "SuccessStop"
                            )
                        ).toEmbed(event.jda)
                    )
                        .setEphemeral(true).queue()
                }) {
                    buttonEvent.hook.sendMessageEmbeds(
                        EmbedManager.getGenericFailure(ConfigManager.config.getString("Embeds.UnexpectedError"))
                            .toEmbed(event.jda)
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
                            ConfigManager.config.getString(
                                CONFIG_PREFIX + "SuccessStart"
                            )
                        ).toEmbed(event.jda)
                    )
                        .setEphemeral(true).queue()
                }) {
                    buttonEvent.hook.sendMessageEmbeds(
                        EmbedManager.getGenericFailure(ConfigManager.config.getString("Messages.Embeds.UnexpectedError"))
                            .toEmbed(event.jda)
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
                        ConfigManager.config.getString(
                            CONFIG_PREFIX + "SuccessRestart"
                        )
                    ).toEmbed(event.jda)
                )
                    .setEphemeral(true).queue()
            }) {
                buttonEvent.hook.sendMessageEmbeds(
                    EmbedManager.getGenericFailure(ConfigManager.config.getString("Embeds.UnexpectedError"))
                        .toEmbed(event.jda)
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
                title = ConfigManager.config.getString(CONFIG_PREFIX + "Modal.Name")
            ) {
                this.short(
                    id = "command",
                    label = "Command",
                    required = true,
                    placeholder = ConfigManager.config.getString(CONFIG_PREFIX + "Modal.Placeholder")
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
        /*Need to retrieve server again*/
        val refreshButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("RefreshType")),
            user = event.user,
            label = getButtonSetting("Refresh"),
            emoji = Emoji.fromUnicode(getButtonSetting("RefreshEmoji")),
            type = CooldownType.REFRESH_BTN
        ) {
            val newButtons = getButtons(server, serverInfo, event)
            val rows = listOf(ActionRow.of(newButtons.subList(0, 5)), ActionRow.of(newButtons.subList(5, newButtons.size)))
            it.editMessageEmbeds(EmbedManager.getServerInfo(serverInfo).toEmbed(event.jda)).setReplace(true)
                .setComponents(rows).queue()
        }
        /*CLOSE BTN*/
        val closeButton = event.jda.cooldownButton(
            style = ButtonStyle.valueOf(getButtonSetting("CloseType")),
            user = event.user,
            label = getButtonSetting("Close"),
            emoji = Emoji.fromUnicode(getButtonSetting("CloseEmoji"))
        ) {
            it.deferEdit().queue()
            it.hook.retrieveOriginal().queue { msg -> msg.delete().queue() }
        }

        return listOf(changeStateButton, restartButton, commandButton, requestLogsButton, refreshButton, closeButton)
    }
}
