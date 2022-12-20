package tech.goksi.pterobot.entities

import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import dev.minn.jda.ktx.util.SLF4J
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.MemoryBar

/*TODO: if node offline server not working*/
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class ServerInfo(private val server: ClientServer) {
    companion object {
        private val logger by SLF4J
    }

    val identifier: String = server.identifier
    val name: String = server.name
    val node: String = server.node
    val primaryAllocation: String = server.primaryAllocation.fullAddress
    private val utilization = try {
        server.retrieveUtilization().execute()
    } catch (exception: Exception) {
        logger.warn("Failed to get utilization for $identifier ! Node probably offline ?")
        null
    }
    val status = utilization?.state?.name ?: "UNKNOWN"
    val cpuUsed = utilization?.cpu ?: 0
    val diskUsed = (utilization?.disk?.toFloat() ?: 0f) / 1024 / 1024 / 1024 // gb
    val ramUsed = (utilization?.memory ?: 0L) / 1024 / 1024 // mb
    val diskMax = (server.limits.diskLong.toFloat()) / 1024
    val ramLimit: Long
        get() {
            return if (server.limits.memoryLong != 0L) server.limits.memoryLong
            else Common.getDefaultApplication().retrieveNodesByName(server.node, false).execute()[0].memoryLong
        }
    val statusEmoji = when (status) {
        "RUNNING" -> NodeStatus.ONLINE.emoji
        "STARTING" -> NodeStatus.ONLINE.emoji
        else -> NodeStatus.OFFLINE.emoji
    }
    val memoryUsageBar: String
        get() = MemoryBar(ramUsed, ramLimit).toString()
}
