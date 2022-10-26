package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.ClientType
import com.mattmalec.pterodactyl4j.UtilizationState
import com.mattmalec.pterodactyl4j.exceptions.HttpException
import com.mattmalec.pterodactyl4j.exceptions.NotFoundException
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.internal.utils.PermissionUtil
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.MemoryBar
import java.util.*
import kotlin.concurrent.fixedRateTimer

private const val CONFIG_PREFIX = "Messages.Commands.NodeInfo."
class NodeInfo(private val dataStorage: DataStorage): SimpleCommand() {
    private val logger by SLF4J
    companion object TaskMapping {
        val mapping: MutableMap<Long, Timer> = HashMap() //message id and timer
    }

    init {
        this.name = "nodeinfo"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
        this.options = listOf(OptionData(OptionType.INTEGER, "id", ConfigManager.config.getString(CONFIG_PREFIX + "OptionDescription"), true),
        OptionData(OptionType.BOOLEAN, "update", ConfigManager.config.getString(CONFIG_PREFIX + "OptionUpdateDescription"), false)
        )
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val nodeId = event.getOption("id")!!.asInt
        val update = event.getOption("update")?.asBoolean ?: false
        val response: MessageEmbed
        var success = false
        if(dataStorage.isPteroAdmin(event.user) || event.member!!.hasPermission(Permission.ADMINISTRATOR)) {
            success = true
            response = try{
                runBlocking {
                    withContext(Dispatchers.IO) { getNodeInfoEmbed(nodeId, event.jda) }
                }
            } catch (exception: Exception){
                when(exception){
                    is HttpException, is NotFoundException -> {
                        success = false
                        logger.debug("Thrown exception: ", exception)
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
                val timer = fixedRateTimer(name = "NodeInfoDaemon#${mapping.size}", daemon = true, period = 300_000, initialDelay = 300_000){
                    it.editMessageEmbeds(runBlocking {
                        withContext(Dispatchers.IO) { getNodeInfoEmbed(nodeId, event.jda) }
                    }).queue() } /*TODO: fixed 5 minutes delay, make configurable*/
                mapping[it.idLong] = timer
            }
        }

    }
    /*TODO: catch login exception ?*/
    private fun getNodeInfoEmbed(id: Int, jda: JDA): MessageEmbed {
        val ptero by lazy {
            Common.getDefaultApplication() to
                Common.createClient(ConfigManager.config.getString("BotInfo.AdminApiKey"))
        }
        val node = ptero.first.retrieveNodeById(id.toLong()).execute()
        var memoryUsed: Long = 0
        var diskSpaceUsed = 0f
        var cpuUsed = 0.0
        var status = NodeStatus.ONLINE
        val runningServers = ptero.second!!.retrieveServers(ClientType.ADMIN_ALL).filter { it.node == node.name }.filter {
            if(it.isInstalling) return@filter false
            if(status == NodeStatus.ONLINE){
                val utilization = try {
                    it.retrieveUtilization().execute()
                }catch (exception: HttpException){
                    status = NodeStatus.OFFLINE
                    return@filter false
                }
                memoryUsed += utilization.memory / 1024 / 1024 //mb
                diskSpaceUsed += utilization.disk.toFloat() / 1024 / 1024 / 1024 //gb
                cpuUsed += utilization.cpu
                return@filter utilization.state == UtilizationState.RUNNING || utilization.state == UtilizationState.STARTING

            } else return@filter false
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
            nodeStatus = status,
            diskMax = (node.diskLong.toFloat()) / 1024,
            diskUsed = diskSpaceUsed,
            cpuUsed = cpuUsed
        ).toEmbed(jda)

    }
}