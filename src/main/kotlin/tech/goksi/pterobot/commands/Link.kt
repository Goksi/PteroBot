package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.exceptions.HttpException
import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import java.sql.SQLException

private const val CONFIG_PREFIX = "Messages.Commands.Link."
class Link(private val dataStorage: DataStorage): SimpleCommand() {
    private val logger by SLF4J
    init {
        this.name = "link"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.options = listOf(OptionData(OptionType.STRING, "apikey", ConfigManager.config.getString(CONFIG_PREFIX + "OptionDescription"), true))
        SendDefaults.ephemeral = ConfigManager.config.getBoolean("BotInfo.Ephemeral")
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        val key = event.getOption("apikey")!!.asString
        val response: MessageEmbed
        if(!dataStorage.isLinked(event.user)){
            response = try{
                if( key.split("_")[0] != "ptlc" || key.length != 48) throw HttpException("Wrong key format !")
                val account = dataStorage.link(event.user, key)
                logger.info("User ${event.user.asTag} linked his discord with ${account.userName} pterodactyl account !")
                EmbedManager.getGenericSuccess(ConfigManager.config.getString(CONFIG_PREFIX + "LinkSuccess").replace("%pteroName", account.userName))
                    .toEmbed(event.jda)
            } catch (exception: SQLException){
                logger.error("Failed to link ${event.user.idLong}", exception)
                EmbedManager.getGenericFailure(ConfigManager.config.getString("Messages.Embeds.UnexpectedError"))
                    .toEmbed(event.jda)
            } catch (httpException: HttpException){
                EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "LinkWrongKey"))
                    .toEmbed(event.jda) //its probably wrong key if we got here, add check maybe
            }
        } else {
            response = EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "LinkExist"))
                .toEmbed(event.jda)
        }
        event.reply_(embeds = listOf(response)).queue()
    }
}