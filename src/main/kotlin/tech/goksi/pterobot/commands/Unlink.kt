package tech.goksi.pterobot.commands

import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed

private const val CONFIG_PREFIX = "Messages.Commands.Unlink."

class Unlink(private val dataStorage: DataStorage) : SimpleCommand() {

    init {
        this.name = "unlink"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        SendDefaults.ephemeral = true
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        if (dataStorage.isLinked(event.user)) {
            dataStorage.unlink(event.user)
            event.reply_(
                embeds = listOf(
                    EmbedManager
                        .getGenericSuccess(ConfigManager.config.getString(CONFIG_PREFIX + "SuccessUnlink"))
                        .toEmbed(event.jda)
                )
            ).queue()
        } else {
            event.reply_(
                embeds = listOf(
                    EmbedManager
                        .getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotLinked"))
                        .toEmbed(event.jda)
                )
            ).queue()
        }
    }
}