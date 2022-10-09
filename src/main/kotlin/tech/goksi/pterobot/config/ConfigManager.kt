package tech.goksi.pterobot.config

import org.simpleyaml.configuration.file.YamlFile
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

object ConfigManager {
    private val configFile: File = File("config.yml")
    private val logger = LoggerFactory.getLogger(ConfigManager::class.java)
    val config: YamlFile

    init {
        if (!configFile.exists()){
            val content = ConfigManager::class.java.classLoader.getResource("config.yml")!!.readText()
            configFile.writeText(content)
        }
        config = YamlFile(configFile)
        try{
            config.loadWithComments()
        } catch (exception: IOException){
            logger.error("Error while loading configuration file, exiting program...", exception)
            exitProcess(1)
        }
    }

    fun save() {
        config.save(configFile)
    }


}