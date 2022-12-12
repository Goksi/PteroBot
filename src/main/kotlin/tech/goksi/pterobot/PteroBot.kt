package tech.goksi.pterobot

import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import tech.goksi.pterobot.commands.*
import tech.goksi.pterobot.commands.manager.SimpleCommandData
import tech.goksi.pterobot.events.NodeStatusDelete
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.util.Checks
import kotlin.system.exitProcess

private const val DEFAULT_NO_TOKEN_MSG = "YOUR TOKEN HERE"
private const val DEFAULT_NO_ID_MSG = "YOUR DISCORD SERVER ID HERE"
private const val DEFAULT_NO_URL_MSG = "YOUR URL HERE"
private const val DEFAULT_NO_API_KEY_MSG = "YOUR PTERODACTYL ADMIN CLIENT KEY HERE"

class PteroBot {
    private val logger by SLF4J
    private val jda: JDA

    /*TODO: DRY*/
    init {
        val tokenPair = Checks.checkInput(
            ConfigManager.config.getString("BotInfo.Token"),
            DEFAULT_NO_TOKEN_MSG,
            "You didn't provide your bot token, please input it right-now: "
        ) { readlnOrNull() }
        val guildPair = Checks.checkInput(
            ConfigManager.config.getString("BotInfo.ServerID"),
            DEFAULT_NO_ID_MSG,
            "You didn't provide your discord server id, please input it right-now: "
        ) {
            var input: String
            while (true) {
                input = readlnOrNull() ?: ""
                if (Checks.validSnowflake(input)) break
                else logger.warn("Invalid server id, please try again !")
            }
            input
        }
        val appUrlPair = Checks.checkInput(
            ConfigManager.config.getString("BotInfo.PterodactylUrl"),
            DEFAULT_NO_URL_MSG,
            "You didn't provide your pterodactyl url, please input it right-now:"
        ) {
            var input: String
            while (true) {
                input = readlnOrNull() ?: ""
                if (Checks.validUrl(input)) break
                else logger.warn("Invalid url, please try again !")
            }
            input
        }
        val apiKeyPair = Checks.checkInput(
            ConfigManager.config.getString("BotInfo.AdminApiKey"),
            DEFAULT_NO_API_KEY_MSG,
            "You didn't provide admin key for actions like register and node info, please input it right-now:"
        ) {
            var input: String
            while (true) {
                input = readlnOrNull() ?: ""
                if (Checks.validClientKey(input)) break
                else logger.warn("Invalid api key, please try again !")
            }
            input
        }
        if (tokenPair.second) {
            ConfigManager.config.set("BotInfo.Token", tokenPair.first)
        }
        if (guildPair.second) {
            ConfigManager.config.set("BotInfo.ServerID", guildPair.first)
        }
        if (appUrlPair.second) {
            ConfigManager.config.set("BotInfo.PterodactylUrl", appUrlPair.first)
        }
        if (apiKeyPair.second) {
            ConfigManager.config.set("BotInfo.AdminApiKey", apiKeyPair.first)
        }
        ConfigManager.save()
        EmbedManager.init()
        jda = default(tokenPair.first!!, enableCoroutines = true, intents = listOf(GatewayIntent.GUILD_MESSAGES)) {
            disableCache(listOf(CacheFlag.VOICE_STATE, CacheFlag.STICKER, CacheFlag.EMOJI, CacheFlag.SCHEDULED_EVENTS))
            val statusStr = ConfigManager.config.getString("BotInfo.Status") ?: "ONLINE".uppercase()
            setStatus(OnlineStatus.valueOf(statusStr))
            if (ConfigManager.config.getBoolean("BotInfo.EnableActivity")) {
                val activityString = ConfigManager.config.getString("BotInfo.ActivityName") ?: ""
                val activityName = ConfigManager.config.getString("BotInfo.Activity") ?: ""
                setActivity(
                    when (activityName.uppercase()) {
                        "LISTENING" -> Activity.listening(activityString)
                        "WATCHING" -> Activity.watching(activityString)
                        else -> Activity.playing(activityString)
                    }
                )
            }
        }.awaitReady()

        val commandData = SimpleCommandData()
        commandData.addCommands(
            Link(),
            NodeInfoCmd(),
            Register(),
            Servers(jda),
            Unlink(),
            NodeStatusCmd()
        )
        val guild = jda.getGuildById(guildPair.first!!) // what if wrong guild id ?
        guild?.updateCommands()?.addCommands(commandData.buildData())?.queue()
        jda.addEventListener(NodeStatusDelete())
        commandData.registerListeners(jda)
        Checks.checkVersion()
        listenStdin()
    }

    private fun listenStdin() {
        logger.info("PteroBot has successfully started, you can stop it by typing \"stop\"")
        while (true) {
            if ((readlnOrNull()?.lowercase()) == "stop") {
                jda.shutdownNow()
                exitProcess(0)
            } else logger.warn("Wrong command ! You mean \"stop\" ?")
        }
    }
}
