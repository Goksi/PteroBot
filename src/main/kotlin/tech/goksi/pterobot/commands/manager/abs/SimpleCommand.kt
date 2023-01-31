package tech.goksi.pterobot.commands.manager.abs

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tech.goksi.pterobot.events.hook.CoroutineListenerAdapter
import tech.goksi.pterobot.util.Checks

/*TODO: probably differ base and sub command*/
abstract class SimpleCommand(
    val name: String,
    val description: String,
    private val subcommands: List<SimpleCommand> = emptyList(),
    private val options: List<OptionData> = emptyList(),
    private val enabledPermissions: List<Permission> = emptyList()
) : CoroutineListenerAdapter() {

    abstract suspend fun execute(event: SlashCommandInteractionEvent)

    override suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (name == event.name) {
            if (subcommands.isEmpty()) execute(event)
            else {
                val subcommand = subcommands.filter { it.name == event.subcommandName }[0]
                subcommand.execute(event)
            }
        }
    }

    fun buildCommand(): SlashCommandData {
        val commandData = Commands.slash(name, description)
        commandData.isGuildOnly = true
        if (enabledPermissions.isNotEmpty()) {
            commandData.defaultPermissions = DefaultMemberPermissions.enabledFor(enabledPermissions)
        }
        if (options.isNotEmpty()) {
            Checks.arguments(options.size <= 25, "Slash command can have max 25 options !")
            commandData.addOptions(options)
        } else { // slash command can't have both subcommands and options
            for (subcommand in subcommands) {
                val subcommandData = SubcommandData(subcommand.name, subcommand.description)
                if (subcommand.options.isNotEmpty()) {
                    Checks.arguments(subcommand.options.size <= 25, "Slash command can have max 25 options !")
                    subcommandData.addOptions(subcommand.options)
                }
                commandData.addSubcommands(subcommandData)
            }
        }
        return commandData
    }
}
