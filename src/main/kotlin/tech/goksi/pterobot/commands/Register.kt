package tech.goksi.pterobot.commands

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.manager.ConfigManager

private const val CONFIG_PREFIX = "Messages.Commands.Register."
class Register: SimpleCommand() {
    private val logger by SLF4J

    init {
        this.name = "register"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false;
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        TODO("Not yet implemented")
    }
}