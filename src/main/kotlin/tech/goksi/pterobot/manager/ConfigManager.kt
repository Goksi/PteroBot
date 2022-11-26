package tech.goksi.pterobot.manager

import dev.minn.jda.ktx.util.SLF4J
import org.simpleyaml.configuration.file.YamlConfiguration
import org.simpleyaml.configuration.file.YamlFile
import org.simpleyaml.utils.SupplierIO
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

object ConfigManager {
    private val configFile: File = File("config.yml")
    private val logger by SLF4J
    val config: YamlFile

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
    }

    fun save() {
        config.save(configFile)
    }
}
