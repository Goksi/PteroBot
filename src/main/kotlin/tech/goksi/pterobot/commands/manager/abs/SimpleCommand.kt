package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.OptionData

abstract class SimpleCommand: ListenerAdapter() {
    lateinit var name: String
    lateinit var description: String
    lateinit var options: List<OptionData>

    abstract fun execute(event: SlashCommandInteractionEvent)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if(name == event.name){
            execute(event)
        }
    }
}