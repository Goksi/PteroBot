package tech.goksi.pterobot.commands.manager

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand

class SimpleCommandData {
    private val commands: MutableList<SimpleCommand>

    init {
        commands = ArrayList()
    }

    fun addCommands(vararg commands: SimpleCommand) {
        this.commands.addAll(commands)
    }

    fun buildData(): List<SlashCommandData> {
        val data: MutableList<SlashCommandData> = ArrayList()
        for (command in this.commands) {
            data.add(command.buildCommand())
        }
        return data
    }

    fun registerListeners(jda: JDA) {
        commands.forEach {
            jda.addEventListener(it)
        }
    }
}
