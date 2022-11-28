package tech.goksi.pterobot.util.cooldown

import net.dv8tion.jda.api.events.GenericEvent
import java.util.concurrent.TimeUnit

enum class CooldownType(private val cooldownSeconds: Int) {
    STATE_BTN(0),
    RESTART_BTN(0),
    COMMAND_BTN(0),
    REFRESH_BTN(0),
    LOGS_BTN(0);

    val millis: Long
        get() = TimeUnit.SECONDS.toMillis(cooldownSeconds.toLong())

    companion object {
        fun fromEvent(event: GenericEvent): CooldownType {

        }
    }
}