package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

abstract class ExecutableCommand<T>(
    name: String,
    description: String,
    options: List<OptionData> = emptyList()
) : CommandBase<T>(name), EventListener where T : SlashCommandData, T : SubcommandData {
    override fun buildCommand(): T {
        if (T is SlashCommandData) {

        }
    }

    abstract fun execute(slashEvent: SlashCommandInteractionEvent)

    override fun onEvent(genericEvent: GenericEvent) {
        if (genericEvent is SlashCommandInteractionEvent) {
            
        }
    }
}