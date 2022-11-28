package tech.goksi.pterobot.util.cooldown

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.util.concurrent.TimeUnit
import kotlin.math.max

object CooldownManager {
    private val cooldownMapping: MutableMap<CooldownNamespace, Long> = HashMap()

    fun applyCooldown(event: ButtonInteractionEvent) {
        val cooldownType = CooldownType.fromEvent(event)
        cooldownMapping[CooldownNamespace(event.idLong,cooldownType)] = System.currentTimeMillis() + cooldownType.millis
    }

    fun canInteract(event: ButtonInteractionEvent): Boolean = getRemaining(event) == 0L


    private fun getRemaining(event: ButtonInteractionEvent): Long =
        max(0, (cooldownMapping[CooldownNamespace(event.idLong, CooldownType.fromEvent(event))] ?: 0) - System.currentTimeMillis())


    fun getRemainingSeconds(event: ButtonInteractionEvent): Long = TimeUnit.MILLISECONDS.toSeconds(getRemaining(event))

}

private data class CooldownNamespace(val id: Long, val type: CooldownType) {}