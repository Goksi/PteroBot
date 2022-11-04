package tech.goksi.pterobot.manager

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.utils.Helpers
import tech.goksi.pterobot.EmbedType
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.entities.NodeInfo
import tech.goksi.pterobot.entities.ServerInfo
import tech.goksi.pterobot.util.MemoryBar
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

object EmbedManager {
    private val logger by SLF4J
    fun init() {
        logger.info("Initializing EmbedManager!")
        EmbedType.values().map { it.path }.forEach {
            val file = File(it)
            if (!file.parentFile.exists()) file.parentFile.mkdir()
            if (!file.exists()) {
                val resourceContent = EmbedManager::class.java.classLoader.getResource(it)!!.readText()
                file.writeText(resourceContent)
            }
        }
    }

    fun getGenericSuccess(message: String): String {
        val rawGeneric by lazy {
            val file = File(EmbedType.GENERIC_SUCCESS.path)
            file.readText()
        }
        return rawGeneric.replace("%message", message)
    }

    fun getGenericFailure(message: String): String {
        val rawGeneric by lazy {
            val file = File(EmbedType.GENERIC_ERROR.path)
            file.readText()
        }
        return rawGeneric.replace("%message", message)
    }

    fun getNodeInfo(nodeInfo: NodeInfo): String {

        val rawNodeInfo by lazy {
            val file = File(EmbedType.NODE_INFO.path)
            file.readText()
        }
        return rawNodeInfo.replace(
            "%runningServers" to nodeInfo.runningServers.toString(),
            "%location" to nodeInfo.location,
            "%maintenance" to nodeInfo.maintenance,
            "%allocationsCount" to nodeInfo.allocationsCount.toString(),
            "%maxMb" to nodeInfo.ramLimit.toString(),
            "%usedMb" to nodeInfo.ramUsed.toString(),
            "%memoryUsageBar" to nodeInfo.memoryBar,
            "%nodeName" to nodeInfo.name,
            "%nodeDescription" to nodeInfo.description,
            "%timestamp" to getCurrentTimestamp(),
            "%statusEmoji" to nodeInfo.status.emoji,
            "%status" to nodeInfo.status.message,
            "%diskMax" to String.format("%.2f", nodeInfo.diskLimit),
            "%diskUsed" to String.format("%.2f", nodeInfo.diskUsed),
            "%cpuUsed" to String.format("%.2f", nodeInfo.cpuUsed)
        )
    }

    fun getServersCommand(
        username: String,
        fullName: String,
        rootAdmin: Boolean,
        email: String
    ): String {
        val rawServersSuccess by lazy {
            val file = File(EmbedType.SERVERS_COMMAND.path)
            file.readText()
        }
        return rawServersSuccess.replace(
            "%pteroName" to username, "%pteroFullName" to fullName, "%isAdmin" to rootAdmin.toString(),
            "%pteroEmail" to email, "%timestamp" to getCurrentTimestamp()
        )
    }

    fun getServerInfo(server: ServerInfo): String {
        val rawServerInfo by lazy {
            val file = File(EmbedType.SERVER_INFO.path)
            file.readText()
        }
        return rawServerInfo.replace(
            "%serverId" to server.identifier,
            "%timestamp" to getCurrentTimestamp(),
            "%serverName" to server.name,
            "%nodeName" to server.node,
            "%primaryAllocation" to server.primaryAllocation,
            "%cpuUsed" to String.format("%.2f", server.cpuUsed),
            "%diskMax" to String.format("%.2f", server.diskMax),
            "%diskUsed" to String.format("%.2f", server.diskUsed),
            "%usedMb" to server.ramUsed.toString(),
            "%maxMb" to server.ramMax.toString(),
            "%memoryUsageBar" to MemoryBar(server.ramUsed, server.ramMax).toString(),
            "%statusEmoji" to server.emoji,
            "%status" to server.status
        )
    }

    fun getNodeStatus(): String {
        val rawServerStatus by lazy {
            val file = File(EmbedType.NODE_STATUS.path)
            file.readText()
        }
        return rawServerStatus.replace("%timestamp" to getCurrentTimestamp())
    }

    fun String.toEmbed(jda: JDA): MessageEmbed {
        val jdaImpl: JDAImpl = jda as JDAImpl
        return jdaImpl.entityBuilder.createMessageEmbed(DataObject.fromJson(this))
    }

    fun String.replace(vararg replacements: Pair<String, String>): String {
        var result = this
        replacements.forEach { (first, second) -> result = result.replace(first, second) }
        return result
    }

    private fun getCurrentTimestamp(): String =
        Helpers.toOffsetDateTime(Instant.now()).format(DateTimeFormatter.ISO_INSTANT)
}