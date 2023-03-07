package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class StandaloneCommand(
    name: String,
    description: String,
    options: List<OptionData>,
    private val enabledPermissions: List<Permission>
) : ExecutableCommand(name, description, options) {
    fun buildCommand(): SlashCommandData {
        val data = Commands.slash(name, description)
        if (options.isNotEmpty()) {
            data.addOptions(options)
        }
        if (enabledPermissions.isNotEmpty()) {
            data.defaultPermissions = DefaultMemberPermissions.enabledFor(enabledPermissions)
        }
        return data
    }
}
