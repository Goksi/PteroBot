package tech.goksi.pterobot.util.cooldown

import dev.minn.jda.ktx.events.onButton
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.jdabuilder.scope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object CooldownManager {
    private val cooldownMapping: MutableMap<CooldownNamespace, Long> = HashMap()

    private fun applyCooldown(event: ButtonInteractionEvent) {
        val cooldownType = CooldownType.fromEvent(event)
        cooldownMapping[CooldownNamespace(event.user.idLong, cooldownType)] =
            System.currentTimeMillis() + cooldownType.millis
    }

    private fun canInteract(event: ButtonInteractionEvent): Boolean = getRemaining(event) == 0L

    private fun getRemaining(event: ButtonInteractionEvent): Long = max(
        0,
        (
            cooldownMapping[CooldownNamespace(event.user.idLong, CooldownType.fromEvent(event))] ?: 0
            ) - System.currentTimeMillis()
    )

    private fun getRemainingSeconds(event: ButtonInteractionEvent): Long =
        TimeUnit.MILLISECONDS.toSeconds(getRemaining(event))

    fun JDA.cooldownButton(
        style: ButtonStyle,
        label: String? = null,
        emoji: Emoji?,
        disabled: Boolean = false,
        expiration: Duration = 15.minutes,
        user: User? = null,
        type: CooldownType = CooldownType.UNDEFINED,
        listener: suspend (ButtonInteractionEvent) -> Unit
    ): Button {
        val id = type.name + ":${ThreadLocalRandom.current().nextLong()}"
        val button = button(id, label, emoji, style, disabled)
        val task = onButton(id, expiration) {
            if (user == null || user == it.user) {
                if (!canInteract(it)) {
                    it.replyEmbeds(
                        EmbedManager.getGenericFailure(
                            ConfigManager.config.getString("Messages.OnCooldown")
                                .replace("%time", "${getRemainingSeconds(it)}")
                        ).toEmbed(it.jda)
                    ).setEphemeral(true).queue()
                    return@onButton
                }
                listener(it)
                applyCooldown(it)
            }
            if (!it.isAcknowledged) it.deferEdit().queue()
        }
        if (expiration.isPositive() && expiration.isFinite()) {
            scope.launch {
                delay(expiration)
                removeEventListener(task)
            }
        }
        return button
    }
}

private data class CooldownNamespace(val id: Long, val type: CooldownType)
