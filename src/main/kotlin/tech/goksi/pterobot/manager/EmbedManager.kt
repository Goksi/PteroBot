package tech.goksi.pterobot.manager

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.entities.MessageEmbed
import tech.goksi.pterobot.EmbedType
import tech.goksi.pterobot.entities.AccountInfo
import tech.goksi.pterobot.entities.NodeInfo
import tech.goksi.pterobot.entities.ServerInfo
import tech.goksi.pterobot.util.EmbedParser
import java.io.File
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility

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
        return rawNodeInfo.replacePlaceholders(getPlaceholderMap(nodeInfo))
    }

    fun getServersCommand(accountInfo: AccountInfo): String {
        val rawServersSuccess by lazy {
            val file = File(EmbedType.SERVERS_COMMAND.path)
            file.readText()
        }
        return rawServersSuccess.replacePlaceholders(getPlaceholderMap(accountInfo))
    }

    fun getServerInfo(server: ServerInfo): String {
        val rawServerInfo by lazy {
            val file = File(EmbedType.SERVER_INFO.path)
            file.readText()
        }
        return rawServerInfo.replacePlaceholders(getPlaceholderMap(server))
    }

    fun getNodeStatus(): String {
        val rawServerStatus by lazy {
            val file = File(EmbedType.NODE_STATUS.path)
            file.readText()
        }
        return rawServerStatus.replace("%timestamp", getCurrentTimestamp().toString())
    }

    fun String.toEmbed(): MessageEmbed {
        return EmbedParser.parse(this)
    }

    private fun String.replacePlaceholders(replacements: Map<String, String>): String {
        var result = this
        replacements.forEach { (key, value) -> result = result.replace("$key\\b".toRegex(), value) }
        return result
    }

    private fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPlaceholderMap(entity: T): Map<String, String> {
        val fields = entity!!::class.members
            .filterIsInstance<KProperty1<*, *>>()
            .map { it as KProperty1<T, *> }.filter { it.visibility != KVisibility.PRIVATE }
        return buildMap {
            this["%timestamp"] = getCurrentTimestamp().toString()
            fields.forEach {
                val value = when (val uncheckedVal = it.get(entity)) {
                    is Float -> String.format("%.2f", uncheckedVal)
                    else -> uncheckedVal.toString()
                }
                this["%${it.name}"] = value
            }
        }
    }
}
