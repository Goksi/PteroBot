package tech.goksi.pterobot.entities

import com.mattmalec.pterodactyl4j.application.entities.Node
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.util.MemoryBar

class NodeInfo(node: Node, val status: NodeStatus, val runningServers: Int, val ramUsed: Long, val diskUsed: Float, val cpuUsed: Double) {
    val name = node.name!!
    val description = node.description ?: ""
    val location = node.retrieveLocation().execute().shortCode!!
    val maintenance = if (node.hasMaintanceMode()) "On" else "Off"
    val allocationsCount = node.retrieveAllocations().execute().size
    val ramLimit = node.memoryLong
    val memoryBar: String
        get() = MemoryBar(ramUsed, ramLimit).toString()
    val diskLimit = (node.diskLong.toFloat()) / 1024
}