package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.ClientType
import com.mattmalec.pterodactyl4j.UtilizationState
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
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
import tech.goksi.pterobot.entities.NodeInfo
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common

private const val NODE_PREFIX = "Messages.Commands.Node"

class NodeCommand : SimpleCommand(
    name = "node",
    description = "Top level node command, have no influence",
    enabledPermissions = listOf(Permission.ADMINISTRATOR),
    subcommands = listOf(Info())
) {

    companion object TaskMapping {
        val coroutineScope by lazy {
            Common.getDefaultCoroutineScope("NodeScope")
        }
        val taskMap: MutableMap<Long, Job> = HashMap()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // base command
    }
}

/*TODO: probably different coroutine scope and error handling*/
private class Info : SimpleCommand(
    name = "info",
    description = ConfigManager.config.getString("$NODE_PREFIX.Info.Description"),
    options = listOf(
        OptionData(
            OptionType.INTEGER,
            "id",
            ConfigManager.config.getString("$NODE_PREFIX.Info.OptionDescription"),
            true
        ),
        OptionData(
            OptionType.BOOLEAN,
            "update",
            ConfigManager.config.getString("$NODE_PREFIX.Info.OptionUpdateDescription"),
            false
        )
    )
) {
    private val logger by SLF4J
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val nodeId = event.getOption("id")!!.asInt
        val update = event.getOption("update")?.asBoolean ?: false
        val response: MessageEmbed
        var success = false
        val pteroMember = PteroMember(event.user)
        if (pteroMember.isPteroAdmin()) {
            success = true
            response = try {
                withContext(Dispatchers.IO) { getNodeInfoEmbed(nodeId, event.jda, pteroMember.client!!) }
            } catch (exception: Exception) {
                when (exception) {
                    is HttpException, is NotFoundException -> {
                        success = false
                        logger.debug("Thrown exception: ", exception)
                        EmbedManager.getGenericFailure(ConfigManager.config.getString("$NODE_PREFIX.Info.NodeNotFound"))
                            .toEmbed(event.jda)
                    } // shame that kotlin doesn't have multi catch
                    else -> throw exception
                }
            }
        } else {
            response = EmbedManager.getGenericFailure(ConfigManager.config.getString("$NODE_PREFIX.Info.NotAdmin"))
                .toEmbed(event.jda)
        }
        event.hook.sendMessageEmbeds(response).queue {
            if (success && update) {
                val job = NodeCommand.coroutineScope.launch {
                    while (true) {
                        delay(300_000)
                        it.editMessageEmbeds(getNodeInfoEmbed(nodeId, event.jda, pteroMember.client!!)).queue()
                    }
                } /*TODO: fixed 5 minutes delay, make configurable*/
                NodeCommand.taskMap[it.idLong] = job
            }
        }
    }

    private fun getNodeInfoEmbed(id: Int, jda: JDA, pteroClient: PteroClient): MessageEmbed {
        val pteroApplication = Common.getDefaultApplication()
        val node = pteroApplication.retrieveNodeById(id.toLong()).execute()
        var memoryUsed: Long = 0
        var diskSpaceUsed = 0f
        var cpuUsed = 0.0
        var status = NodeStatus.ONLINE
        val runningServers =
            pteroClient.retrieveServers(ClientType.ADMIN_ALL).filter { it.node == node.name }.filter {
                if (it.isInstalling) return@filter false
                if (status == NodeStatus.ONLINE) {
                    val utilization = try {
                        it.retrieveUtilization().execute()
                    } catch (exception: HttpException) {
                        status = NodeStatus.OFFLINE
                        return@filter false
                    }
                    memoryUsed += utilization.memory / 1024 / 1024 // mb
                    diskSpaceUsed += utilization.disk.toFloat() / 1024 / 1024 / 1024 // gb
                    cpuUsed += utilization.cpu
                    return@filter utilization.state == UtilizationState.RUNNING || utilization.state == UtilizationState.STARTING
                } else return@filter false
            }

        return EmbedManager.getNodeInfo(
            NodeInfo(
                node = node,
                nodeStatus = status,
                runningServers = runningServers.size,
                ramUsed = memoryUsed,
                diskUsed = diskSpaceUsed,
                cpuUsed = cpuUsed
            )
        ).toEmbed(jda)
    }
}
