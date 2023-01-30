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
import tech.goksi.pterobot.entities.ApiKey
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Checks
import tech.goksi.pterobot.util.Common
import java.sql.SQLException

private const val ACCOUNT_PREFIX = "Messages.Commands.Account"

class AccountCommand : SimpleCommand(
    name = "account",
    description = "Main account command, have no influence",
    subcommands = listOf(Link(), Unlink())
) {

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Base command
    }
}

private class Link : SimpleCommand(
    name = "link",
    description = ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.Description"),
    options = listOf(
        OptionData(
            OptionType.STRING,
            "apikey",
            ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.OptionDescription"),
            true
        )
    )
) {
    private val logger by SLF4J

    init {
        SendDefaults.ephemeral = true
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val key = event.getOption("apikey")!!.asString
        val response: MessageEmbed
        val pteroMember = PteroMember(event.user)
        if (!pteroMember.isLinked()) {
            response = try {
                if (!Checks.validClientKey(key)) throw HttpException("Wrong key format !")
                val pteroAccount = Common.createClient(key)!!
                val account = pteroAccount.retrieveAccount().execute()
                pteroMember.link(ApiKey(key, account.isRootAdmin))
                logger.info("User ${event.user.asTag} linked his discord with ${account.userName} pterodactyl account !")
                EmbedManager.getGenericSuccess(
                    ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.LinkSuccess")
                        .replace("%pteroName", account.userName)
                )
                    .toEmbed(event.jda)
            } catch (exception: SQLException) {
                logger.error("Failed to link ${event.user.idLong}", exception)
                EmbedManager.getGenericFailure(ConfigManager.config.getString("Messages.Embeds.UnexpectedError"))
                    .toEmbed(event.jda)
            } catch (httpException: HttpException) {
                EmbedManager.getGenericFailure(ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.LinkWrongKey"))
                    .toEmbed(event.jda) // its probably wrong key if we got here, add check maybe
            }
        } else {
            response =
                EmbedManager.getGenericFailure(ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.LinkExist"))
                    .toEmbed(event.jda)
        }
        event.reply_(embeds = listOf(response)).queue()
    }
}

private class Unlink : SimpleCommand(
    name = "unlink",
    description = ConfigManager.config.getString("${ACCOUNT_PREFIX}.Unlink.Description")
) {

    init {
        SendDefaults.ephemeral = true
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val pteroMember = PteroMember(event.user)
        if (pteroMember.isLinked()) {
            pteroMember.unlink()
            event.reply_(
                embeds = listOf(
                    EmbedManager
                        .getGenericSuccess(ConfigManager.config.getString("${ACCOUNT_PREFIX}.Unlink.SuccessUnlink"))
                        .toEmbed(event.jda)
                )
            ).queue()
        } else {
            event.reply_(
                embeds = listOf(
                    EmbedManager
                        .getGenericFailure(ConfigManager.config.getString("${ACCOUNT_PREFIX}.Unlink.NotLinked"))
                        .toEmbed(event.jda)
                )
            ).queue()
        }
    }
}
