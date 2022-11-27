package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.events.hook.CoroutineListenerAdapter

abstract class SimpleCommand : CoroutineListenerAdapter() {
    lateinit var name: String
    lateinit var description: String
    var options: List<OptionData> = emptyList()
    var enabledPermissions: List<Permission> = emptyList()
    var enableDefault: Boolean = true

    abstract suspend fun execute(event: SlashCommandInteractionEvent)

    override suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (name == event.name) {
            execute(event)
        }
    }
}
