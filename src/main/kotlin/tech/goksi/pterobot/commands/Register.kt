package tech.goksi.pterobot.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.config.ConfigManager
const val CONFIG_PREFIX = "Messages.Commands.Register."
class Register: SimpleCommand() {
    init {
        this.name = "connect"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.options = listOf(OptionData(OptionType.STRING, "apikey", ConfigManager.config.getString(CONFIG_PREFIX + "OptionDescription"), true))
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        TODO("Not yet implemented")
    }
}