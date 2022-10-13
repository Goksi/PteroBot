package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.ClientType
import com.mattmalec.pterodactyl4j.UtilizationState
import com.mattmalec.pterodactyl4j.exceptions.HttpException
import com.mattmalec.pterodactyl4j.exceptions.NotFoundException
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.MemoryBar
import java.io.IOException
import java.net.URL
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
/*TODO: more info like cpu usage or disk usage*/
private const val CONFIG_PREFIX = "Messages.Commands.NodeInfo."
class NodeInfo(private val dataStorage: DataStorage): SimpleCommand() {
    private val logger by SLF4J
    companion object TaskMapping {
        val mapping: MutableMap<Long, Timer> = HashMap() //message id and timer
    }

    init {
        this.name = "nodeinfo"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false;
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
        this.options = listOf(OptionData(OptionType.INTEGER, "id", ConfigManager.config.getString(CONFIG_PREFIX + "OptionDescription"), true),
        OptionData(OptionType.BOOLEAN, "update", ConfigManager.config.getString(CONFIG_PREFIX + "OptionUpdateDescription"), false)
        )
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val nodeId = event.getOption("id")!!.asInt
        val update = if(event.getOption("update") != null) event.getOption("update")!!.asBoolean else false
        val response: MessageEmbed
        var success = false
        if(dataStorage.isPteroAdmin(event.user)){
            success = true
            response = try{
                getNodeInfoEmbed(nodeId, event.jda)
            } catch (exception: Exception){
                when(exception){
                    is HttpException, is NotFoundException -> {
                        success = false
                        EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NodeNotFound")).toEmbed(event.jda)
                    } //shame that kotlin doesn't have multi catch
                    else -> throw exception
                }
            }

        } else {
            response = EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotAdmin")).toEmbed(event.jda)
        }
        event.hook.sendMessageEmbeds(response).queue {
            if(success && update){
                val timer = fixedRateTimer(name = "NodeInfoDaemon#${mapping.size}", daemon = true, period = 300_000){
                    it.editMessageEmbeds(getNodeInfoEmbed(nodeId, event.jda)).queue() } /*TODO: fixed 5 minutes delay, make configurable*/
                mapping[it.idLong] = timer
            }
        }
    }

    private fun getNodeInfoEmbed(id: Int, jda: JDA): MessageEmbed {
        val ptero by lazy {
            Pair(Common.createApplication(ConfigManager.config.getString("BotInfo.AdminApiKey")),
                Common.createClient(ConfigManager.config.getString("BotInfo.AdminApiKey")))
        }
        val node = ptero.first.retrieveNodeById(id.toLong()).execute()
        var memoryUsed: Long = 0
        val runningServers = ptero.second.retrieveServers(ClientType.ADMIN_ALL).filter { it.node == node.name }.filter {
            if(it.isInstalling) return@filter false
            val utilization = it.retrieveUtilization().execute()
            memoryUsed += utilization.memory / 1024 / 1024
            return@filter utilization.state == UtilizationState.RUNNING || utilization.state == UtilizationState.STARTING
        }
        val nodeUrl = "${node.scheme}://${node.fqdn}:${node.daemonListenPort}"
        val url = URL(nodeUrl)
        val status = try{
            if(url.readText() == "{\"error\":\"The required authorization heads were not present in the request.\"}") NodeStatus.ONLINE
            else NodeStatus.OFFLINE
        } catch (exception: IOException){
            if(exception.message?.contains("401") == true) NodeStatus.ONLINE
            else NodeStatus.OFFLINE
        }



        return EmbedManager.getNodeInfo(
            nodeName = node.name,
            nodeDescription = node.description,
            location = node.retrieveLocation().execute().shortCode,
            maintenance = node.hasMaintanceMode(),
            allocationCount = node.retrieveAllocations().execute().size,
            maxMb = node.memoryLong,
            runningServers = runningServers.size,
            usedMb = memoryUsed,
            memoryBar = MemoryBar(memoryUsed, node.memoryLong).toString(),
            nodeStatus = status
        ).toEmbed(jda)

    }
}