package tech.goksi.pterobot.entities

import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import tech.goksi.pterobot.NodeStatus

data class ServerInfo(private val server: ClientServer) {
    val identifier: String = server.identifier
    val name: String = server.name
    val node: String = server.node
    val primaryAllocation: String = server.primaryAllocation.fullAddress
    private val utilization = server.retrieveUtilization().execute()
    val status = utilization.state.name
    val cpuUsed = utilization.cpu
    val diskUsed = (utilization.disk.toFloat()) / 1024 / 1024 / 1024  //gb
    val ramUsed = utilization.memory / 1024 / 1024 //mb
    val diskMax = (server.limits.diskLong.toFloat()) / 1024
    val ramMax = server.limits.memoryLong
    val emoji = when (status) {
        "RUNNING" -> NodeStatus.ONLINE.emoji
        "STARTING" -> NodeStatus.ONLINE.emoji
        else -> NodeStatus.OFFLINE.emoji
    }
}
