package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

abstract class CommandBase<T>(name: String) where T : SlashCommandData, T : SubcommandData {
    abstract fun buildCommand(): T
}
