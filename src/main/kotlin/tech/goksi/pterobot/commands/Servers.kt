package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.PowerAction
import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.exceptions.LoginException
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onComponent
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.SelectMenu
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.ModalInteraction
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.entities.ServerInfo
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import kotlin.time.Duration.Companion.minutes

private const val CONFIG_PREFIX = "Messages.Commands.Servers."
private const val SELECTION_ID = "pterobot:servers-selector"

class Servers(private val dataStorage: DataStorage, jda: JDA): SimpleCommand() {
    private val logger by SLF4J
    private val serverMapping = HashMap<String, ClientServer>()
    init {
        this.name = "servers"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
    }
    /*LISTENER FOR COMMAND*/
    init {
        jda.listener<ModalInteractionEvent>(timeout = 2.minutes){
            val id = it.modalId
            if(id.startsWith("pterobot:command")){
                val server = serverMapping[id.split(":")[2]]!!.also { serverMapping.remove(id.split(":")[2]) }
                server.sendCommand(it.getValue("command")!!.asString).executeAsync({_ ->
                    /*TODO: configure*/
                    it.replyEmbeds(EmbedManager.getGenericSuccess("bravo").toEmbed(it.jda)).setEphemeral(true).queue()
                }){throwable ->
                    it.replyEmbeds(EmbedManager.getGenericFailure("grijeska").toEmbed(it.jda)).setEphemeral(true).queue()
                    logger.error("Error while sending command to ${server.name}", throwable)
                }
            }
        }
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        /*TODO: probably remove this, shouldn't require two database calls*/
        if(dataStorage.isLinked(event.user)){
            val pteroClient = dataStorage.getClient(event.user)
            val servers = try{
                pteroClient.retrieveServers().execute()
            } catch (exception: LoginException){
                event.hook.sendMessageEmbeds(EmbedManager
                    .getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotFound")).toEmbed(event.jda)).queue()
                return
            }
            val selectMenu = SelectMenu(SELECTION_ID + ":${event.user.idLong}"){
                for(server in servers){
                    this.option(label = server.name, value = server.identifier)
                }
                this.placeholder = ConfigManager.config.getString(CONFIG_PREFIX + "MenuPlaceholder")
            }
            val pteroAccount = pteroClient.retrieveAccount().execute()

            val response = EmbedManager.getServersCommand(username = pteroAccount.userName,
            fullName = pteroAccount.fullName, rootAdmin = pteroAccount.isRootAdmin, email = pteroAccount.email).toEmbed(event.jda)

            event.hook.sendMessageEmbeds(response).addActionRow(selectMenu).queue()

        } else {
            event.hook.sendMessageEmbeds(EmbedManager
                .getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotLinked")).toEmbed(event.jda)).queue()
        }
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent) {
        if(!event.componentId.startsWith(SELECTION_ID)) return
        if(event.componentId.split(":")[2] != event.user.id){
            event.replyEmbeds(EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "WrongUser")).toEmbed(event.jda))
                .setEphemeral(true).queue()
            return
        }
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        if(!event.message.isEphemeral) event.message.delete().queue()
        val pteroClient = dataStorage.getClient(event.user)
        val server =  try {
            pteroClient.retrieveServerByIdentifier(event.selectedOptions[0].value).execute()
        }catch (exception: LoginException){
            event.hook.sendMessageEmbeds(EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "WrongKey")).toEmbed(event.jda)).queue()
            return
        }
        val serverInfo = ServerInfo(server)
        val response = EmbedManager.getServerInfo(serverInfo).toEmbed(event.jda)
        /*START OR STOP BTN*/
        val changeStateButton = when(serverInfo.status){
            "RUNNING", "STARTING" -> event.jda.button(style = ButtonStyle.valueOf(getButtonSetting("StopType")),
                user = event.user, label = getButtonSetting("Stop"), emoji = Emoji.fromUnicode(getButtonSetting("StopEmoji"))){buttonEvent ->
                server.setPower(PowerAction.STOP).executeAsync({
                    buttonEvent.hook.sendMessageEmbeds(EmbedManager.getGenericSuccess(ConfigManager.config.getString(CONFIG_PREFIX + "SuccessStop")).toEmbed(event.jda))
                        .setEphemeral(true).queue()
                }) {
                    buttonEvent.hook.sendMessageEmbeds(EmbedManager.getGenericFailure(ConfigManager.config.getString("Embeds.UnexpectedError")).toEmbed(event.jda))
                        .setEphemeral(true).queue().also {_ ->
                            logger.error("Error while changing server state !", it)
                        }
                }
            }
            else -> event.jda.button(style = ButtonStyle.valueOf(getButtonSetting("StartType")),
                user = event.user, label = getButtonSetting("Start"), emoji = Emoji.fromUnicode(getButtonSetting("StartEmoji"))){buttonEvent ->
                server.setPower(PowerAction.START).executeAsync({
                    buttonEvent.hook.sendMessageEmbeds(EmbedManager.getGenericSuccess(ConfigManager.config.getString(CONFIG_PREFIX + "SuccessStart")).toEmbed(event.jda))
                        .setEphemeral(true).queue()
                }) {
                    buttonEvent.hook.sendMessageEmbeds(EmbedManager.getGenericFailure(ConfigManager.config.getString("Embeds.UnexpectedError")).toEmbed(event.jda))
                        .setEphemeral(true).queue().also {_ ->
                            logger.error("Error while changing server state !", it)
                        }
                }
            }
        }
        /*RESTART BTN*/
        val restartButton = event.jda.button(style = ButtonStyle.valueOf(getButtonSetting("RestartType")),
            user = event.user, label = getButtonSetting("Restart"), emoji = Emoji.fromUnicode(getButtonSetting("RestartEmoji"))){buttonEvent ->
            server.setPower(PowerAction.RESTART).executeAsync({
                buttonEvent.hook.sendMessageEmbeds(EmbedManager.getGenericSuccess(ConfigManager.config.getString(CONFIG_PREFIX + "SuccessRestart")).toEmbed(event.jda))
                    .setEphemeral(true).queue()
            }) {
                buttonEvent.hook.sendMessageEmbeds(EmbedManager.getGenericFailure(ConfigManager.config.getString("Embeds.UnexpectedError")).toEmbed(event.jda))
                    .setEphemeral(true).queue().also {_ ->
                        logger.error("Error while changing server state !", it)
                    }

            }
        }
        /*COMMAND BTN*/
        val commandButton = event.jda.button(style = ButtonStyle.valueOf(getButtonSetting("CommandType")),
        user = event.user, label = getButtonSetting("Command"), emoji = Emoji.fromUnicode(getButtonSetting("CommandEmoji"))){ buttonEvent ->
            /*TODO: config*/
            val commandModal = Modal(id = "pterobot:command:${server.identifier}", title = "Send command"){
                this.short("command", "Command", true, null, "Please enter your command here")
            }
            buttonEvent.replyModal(commandModal).queue()
            serverMapping[server.identifier] = server
        }
        /*CLOSE BTN*/
        val closeButton = event.jda.button(style = ButtonStyle.valueOf(getButtonSetting("CloseType")),
            user = event.user, label = getButtonSetting("Close"), emoji = Emoji.fromUnicode(getButtonSetting("CloseEmoji"))){
            it.deferEdit().queue()
            val original = it.hook.retrieveOriginal().await()
            if(!original.isEphemeral) original.delete().queue()
        }
        event.hook.sendMessageEmbeds(response).addActionRow(changeStateButton, restartButton, if(serverInfo.status == "RUNNING") commandButton else commandButton.asDisabled(), closeButton).queue()
    }

    private fun getButtonSetting(setting: String) = ConfigManager.config.getString(CONFIG_PREFIX + "Buttons.$setting")
}