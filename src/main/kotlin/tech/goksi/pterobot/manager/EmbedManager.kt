package tech.goksi.pterobot.manager

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tech.goksi.pterobot.EmbedType
import java.io.File

object EmbedManager {
    private val logger: Logger = LoggerFactory.getLogger(EmbedManager::class.java)
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

    fun getNodeInfo(runningServers: Int, locationId: Int, maintenance: Boolean, allocationCount: Int, maxMb: Int, usedMb: Int, memoryBar: String): String {
        val rawNodeInfo by lazy {
            val file = File(EmbedType.NODE_INFO.path)
            file.readText()
        }
        return rawNodeInfo.replace("%runningServers" to runningServers.toString(), "%locationId" to locationId.toString(),
        "%maintenance" to if(maintenance) "On" else "Off", "%allocationsCount" to allocationCount.toString(), "%maxMb" to maxMb.toString(),
        "%usedMb" to usedMb.toString(), "%memoryUsageBar" to memoryBar)
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