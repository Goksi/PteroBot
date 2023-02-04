package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class TopLevelCommand(
    override val name: String,
    private val subcommands: List<SimpleSubcommand>,
    private val enabledPermissions: List<Permission> = emptyList()
) : CommandBase {
    fun buildCommand(): SlashCommandData {
        val data = Commands.slash(name, " ")
        data.addSubcommands(subcommands.map { it.buildSubcommand() })
        if (enabledPermissions.isNotEmpty()) {
            data.defaultPermissions = DefaultMemberPermissions.enabledFor(enabledPermissions)
        }
        return data
    }
}