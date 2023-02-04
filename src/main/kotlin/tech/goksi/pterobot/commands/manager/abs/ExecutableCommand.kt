package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.build.OptionData

abstract class ExecutableCommand(
    override val name: String,
    val description: String,
    val options: List<OptionData>
) : CommandBase, EventListener {

    abstract fun execute(slashEvent: SlashCommandInteractionEvent)

    open fun shouldExecute(slashEvent: SlashCommandInteractionEvent): Boolean {
        return name == slashEvent.name
    }

    override fun onEvent(genericEvent: GenericEvent) {
        if (genericEvent is SlashCommandInteractionEvent) {
            onSlashCommandInteraction(genericEvent)
        }
    }

    private fun onSlashCommandInteraction(slashEvent: SlashCommandInteractionEvent) {
        if (shouldExecute(slashEvent)) {
            execute(slashEvent)
        }
    }
}