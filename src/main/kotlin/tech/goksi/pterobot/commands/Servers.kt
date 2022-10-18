package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.exceptions.LoginException
import dev.minn.jda.ktx.interactions.components.SelectMenu
import dev.minn.jda.ktx.interactions.components.option
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed

private const val CONFIG_PREFIX = "Messages.Commands.Servers."
private const val SELECTION_ID = "pterobot:servers-selector"

class Servers(private val dataStorage: DataStorage): SimpleCommand() {

    init {
        this.name = "servers"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()
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
            val selectMenu = SelectMenu(SELECTION_ID){
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
        if(event.componentId != SELECTION_ID) return
        val pteroClient = dataStorage.getClient(event.user)
        val server = pteroClient.retrieveServerByIdentifier(event.selectedOptions[0].value).execute() //catch exception
    }
}