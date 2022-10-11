package tech.goksi.pterobot.commands

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
/*TODO: maybe all nodes info*/
private const val CONFIG_PREFIX = "Messages.Commands.NodeInfo."
class NodeInfo(private val dataStorage: DataStorage): SimpleCommand() {

    companion object TaskMapping {
        val mapping: MutableMap<Long, Timer> = HashMap() //message id and timer
    }

    init {
        this.name = "nodeinfo"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false;
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
        this.options = listOf(OptionData(OptionType.INTEGER, "id", ConfigManager.config.getString(CONFIG_PREFIX + "OptionDescription"), true))
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply()
        val nodeId = event.getOption("id")!!.asInt
        var response: MessageEmbed;
        var success = false
        if(dataStorage.isPteroAdmin(event.user)){
            success = true
            response = getNodeInfoEmbed(nodeId)
        } else {
            response = EmbedManager.getGenericFailure(CONFIG_PREFIX + "NotAdmin").toEmbed(event.jda)
        }
        event.hook.sendMessageEmbeds(response).queue {
            if(success){
                val timer = fixedRateTimer(name = "NodeInfoDaemon#${mapping.size}", daemon = true, period = 300_000){
                    it.editMessageEmbeds(getNodeInfoEmbed(nodeId)) } /*TODO: fixed 5 minutes delay, make configurable*/
                mapping[it.idLong] = timer
            }
        }
    }

    private fun getNodeInfoEmbed(id: Int): MessageEmbed {
        /*TODO("IMPL")*/
    }
}