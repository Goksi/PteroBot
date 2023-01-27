package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.events.hook.CoroutineListenerAdapter

abstract class SimpleCommand(
    val name: String,
    val description: String,
    val options: List<OptionData> = emptyList(),
    val enabledPermissions: List<Permission> = emptyList()
) : CoroutineListenerAdapter() {

    abstract suspend fun execute(event: SlashCommandInteractionEvent)

    override suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (name == event.name) {
            execute(event)
        }
    }
}
