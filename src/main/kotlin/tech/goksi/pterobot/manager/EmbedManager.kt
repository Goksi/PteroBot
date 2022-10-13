package tech.goksi.pterobot.manager

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import tech.goksi.pterobot.EmbedType
import tech.goksi.pterobot.NodeStatus
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object EmbedManager {
    private val logger by SLF4J
    fun init(){
        logger.info("Initializing EmbedManager!")
        EmbedType.values().map { it.path }.forEach{
            val file = File(it)
            if(!file.parentFile.exists()) file.parentFile.mkdir()
            if(!file.exists()){
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
    /*TODO: statuses of node*/
    fun getNodeInfo(nodeName: String,
                    nodeDescription: String,
                    nodeStatus: NodeStatus,
                    runningServers: Int,
                    location: String,
                    maintenance: Boolean,
                    allocationCount: Int,
                    maxMb: Long,
                    usedMb: Long,
                    memoryBar: String): String {

        val rawNodeInfo by lazy {
            val file = File(EmbedType.NODE_INFO.path)
            file.readText()
        }
        val localTime = LocalDateTime.now()
        val offsetData = OffsetDateTime.of(localTime, OffsetDateTime.now().offset)
        return rawNodeInfo.replace("%runningServers" to runningServers.toString(), "%location" to location,
        "%maintenance" to if(maintenance) "On" else "Off", "%allocationsCount" to allocationCount.toString(), "%maxMb" to maxMb.toString(),
        "%usedMb" to usedMb.toString(), "%memoryUsageBar" to memoryBar, "%nodeName" to nodeName, "%nodeDescription" to nodeDescription,
        "%timestamp" to offsetData.format(DateTimeFormatter.ISO_DATE_TIME), "%statusEmoji" to nodeStatus.emoji, "%status" to nodeStatus.message)
    }

    fun String.toEmbed(jda: JDA): MessageEmbed {
        val jdaImpl: JDAImpl = jda as JDAImpl
        return jdaImpl.entityBuilder.createMessageEmbed(DataObject.fromJson(this))
    }

    private fun String.replace(vararg replacements: Pair<String, String>): String {
        var result = this
        replacements.forEach { (l, r) -> result = result.replace(l, r) }
        return result
    }
}