package tech.goksi.pterobot.util.cooldown

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import tech.goksi.pterobot.manager.ConfigManager
import java.util.concurrent.TimeUnit

private const val BUTTON_CONFIG = "Cooldown.Button."

enum class CooldownType(coolDownConfig: String) {
    STATUS_BTN("StatusChange"),
    RESTART_BTN("RestartServer"),
    COMMAND_BTN("SendCommand"),
    REFRESH_BTN("RefreshEmbed"),
    LOGS_BTN("RequestLogs"),
    UNDEFINED("");

    private val seconds: Long = ConfigManager.config.getLong("$BUTTON_CONFIG$coolDownConfig")
    val millis
        get() = seconds * 1000

    companion object {
        fun fromEvent(event: ButtonInteractionEvent): CooldownType = valueOf(event.button.id!!.split(":")[0])

        fun fromEvent(event: SlashCommandInteractionEvent): CooldownType {
            TODO("Not impl")
        }
    }
}
