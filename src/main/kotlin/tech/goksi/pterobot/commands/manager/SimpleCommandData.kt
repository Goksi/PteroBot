package tech.goksi.pterobot.commands.manager

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.util.Checks

class SimpleCommandData {
    private val commands: MutableList<SimpleCommand>
    init {
        commands = ArrayList()
    }


    fun addCommands(vararg commands: SimpleCommand){
        this.commands.addAll(commands)
    }

    fun buildData(): List<SlashCommandData> {
        val data: MutableList<SlashCommandData> = ArrayList()
        for(command in this.commands){
            Checks.arguments(command.options.size <= 25, "Slash command can have max 25 options !")
            val cmdData = Commands.slash(command.name, command.description)
            cmdData.isGuildOnly = true
            if(!command.enableDefault){
                cmdData.defaultPermissions = DefaultMemberPermissions.enabledFor(command.enabledPermissions)
            }
            if(command.options.isNotEmpty()) cmdData.addOptions(command.options)
            data.add(cmdData)
        }
        return data
    }

    fun registerListeners(jda: JDA){
        commands.forEach{
            jda.addEventListener(it)
        }
    }
}