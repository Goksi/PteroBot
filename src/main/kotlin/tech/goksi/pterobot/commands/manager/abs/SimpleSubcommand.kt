package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

abstract class SimpleSubcommand(
    name: String,
    description: String,
    options: List<OptionData> = emptyList(),
    private val baseCommand: String
) : ExecutableCommand(name, description, options) {

    override fun shouldExecute(event: SlashCommandInteractionEvent): Boolean {
        return "$baseCommand $name" == event.fullCommandName
    }

    fun buildSubcommand(): SubcommandData {
        val data = SubcommandData(name, description)
        if (options.isNotEmpty()) {
            data.addOptions(options)
        }
        return data
    }
}
