package tech.goksi.pterobot.commands.manager

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import tech.goksi.pterobot.commands.manager.abs.CommandBase
import tech.goksi.pterobot.commands.manager.abs.StandaloneCommand
import tech.goksi.pterobot.commands.manager.abs.TopLevelCommand

class SimpleCommandData {
    private val commands: MutableList<CommandBase>

    init {
        commands = ArrayList()
    }

    fun addCommands(vararg commands: CommandBase) {
        this.commands.addAll(commands)
    }

    fun buildData(): List<SlashCommandData> {
        val data: MutableList<SlashCommandData> = ArrayList()
        for (command in this.commands) {
            if (command is TopLevelCommand) data.add(command.buildCommand()) // TODO: this is quite a shitty
            else if (command is StandaloneCommand) data.add(command.buildCommand())
        }
        return data
    }

    fun registerListeners(jda: JDA) {
        commands.forEach {
            if (it is TopLevelCommand) {
                it.subcommands.forEach(jda::addEventListener)
            } else jda.addEventListener(it)
        }
    }
}
