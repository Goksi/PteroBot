package tech.goksi.pterobot

import org.slf4j.LoggerFactory
import tech.goksi.pterobot.config.ConfigManager
import dev.minn.jda.ktx.jdabuilder.default
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.utils.cache.CacheFlag
import tech.goksi.pterobot.commands.Register
import tech.goksi.pterobot.commands.manager.SimpleCommandData
import tech.goksi.pterobot.util.Common

const val DEFAULT_NO_TOKEN_MSG = "YOUR TOKEN HERE"
const val DEFAULT_NO_ID_MSG = "YOUR DISCORD SERVER ID HERE"

class PteroBot(args: Array<String>) {
    private val logger = LoggerFactory.getLogger(PteroBot::class.java)
    private val jda: JDA

    init {
        val tokenPair = Common.checkInput(ConfigManager.config.getString("BotInfo.Token"), DEFAULT_NO_TOKEN_MSG,
        "You didn't provide your bot token, please input it right-now: "
        ) { readLine() }
        val guildPair = Common.checkInput(ConfigManager.config.getString("BotInfo.ServerID"), DEFAULT_NO_ID_MSG,
            "You didn't provide your discord server id, please input it right-now: "
        ) { readLine() }
        if(tokenPair.second){
            ConfigManager.config.set("BotInfo.Token", tokenPair.first)
            ConfigManager.save()
        }
        if(guildPair.second){
            ConfigManager.config.set("BotInfo.ServerID", guildPair.first)
            ConfigManager.save()
        }
        jda = default(tokenPair.first!!, enableCoroutines = true, intents = emptyList()){
            disableCache(listOf(CacheFlag.VOICE_STATE, CacheFlag.STICKER, CacheFlag.EMOJI))
            val statusStr = ConfigManager.config.getString("BotInfo.Status")?:"".uppercase()
            setStatus(when (statusStr) {
                "INVISIBLE" -> OnlineStatus.INVISIBLE
                "DND" -> OnlineStatus.DO_NOT_DISTURB
                "IDLE" -> OnlineStatus.IDLE
                else -> OnlineStatus.ONLINE
                })
            if(ConfigManager.config.getBoolean("BotInfo.EnableActivity")){
                val activityString = ConfigManager.config.getString("BotInfo.ActivityName")?:""
                val activityName = ConfigManager.config.getString("BotInfo.Activity")?:"".uppercase()
                setActivity(when (activityName){
                    "LISTENING" -> Activity.listening(activityString)
                    "WATCHING" -> Activity.watching(activityString)
                    else -> Activity.playing(activityString)
                })
            }

        }.awaitReady()

        val commandData = SimpleCommandData()
        commandData.addCommands(Register())
        val guild = jda.getGuildById(guildPair.first!!)
        guild?.updateCommands()?.addCommands(commandData.buildData())?.queue()
        commandData.registerListeners(jda)
    }
}