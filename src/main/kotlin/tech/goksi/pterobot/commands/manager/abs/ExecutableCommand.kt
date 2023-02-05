package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.events.hook.CoroutineListenerAdapter

abstract class ExecutableCommand(
    override val name: String,
    val description: String,
    val options: List<OptionData>
) : CoroutineListenerAdapter(), CommandBase {

    abstract suspend fun execute(event: SlashCommandInteractionEvent)

    open fun shouldExecute(event: SlashCommandInteractionEvent): Boolean {
        return name == event.name
    }

    override suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (shouldExecute(event)) {
            execute(event)
        }
    }
}
