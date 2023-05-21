package tech.goksi.pterobot.manager

import dev.minn.jda.ktx.util.SLF4J
import org.simpleyaml.configuration.file.YamlConfiguration
import org.simpleyaml.configuration.file.YamlFile
import org.simpleyaml.configuration.serialization.ConfigurationSerialization
import org.simpleyaml.utils.SupplierIO
import tech.goksi.pterobot.entities.ButtonInfo
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

object ConfigManager {
    private val configFile: File = File("config.yml")
    private val logger by SLF4J
    private val config: YamlFile

    init {
        val inputStreamURL = ConfigManager::class.java.classLoader.getResource("config.yml")!!
        if (!configFile.exists()) {
            val content = inputStreamURL.readText()
            configFile.writeText(content)
        }
        config = YamlFile(configFile)
        try {
            config.loadWithComments()
            config.defaults =
                YamlConfiguration.loadConfiguration(SupplierIO.InputStream { inputStreamURL.openStream() })
        } catch (exception: IOException) {
            logger.error("Error while loading configuration file, exiting program...", exception)
            exitProcess(1)
        }
        ConfigurationSerialization.registerClass(ButtonInfo::class.java)
    }

    fun getString(path: String): String = config.getString(path)

    fun getBoolean(path: String) = config.getBoolean(path)

    fun getInt(path: String) = config.getInt(path)

    fun getLong(path: String) = config.getLong(path)

    fun getButtonInfo(path: String) = config.get(path) as ButtonInfo

    fun set(path: String, value: Any?) = config.set(path, value)

    fun save() = config.save(configFile)
}
