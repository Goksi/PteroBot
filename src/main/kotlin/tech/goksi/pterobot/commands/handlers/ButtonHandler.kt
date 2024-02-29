package tech.goksi.pterobot.commands.handlers

import dev.minn.jda.ktx.interactions.components.button
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import tech.goksi.pterobot.entities.ButtonInfo

abstract class ButtonHandler(
    protected val jda: JDA,
    buttonInfo: ButtonInfo,
    user: User? = null,
    enabled: Boolean = true
) {
    val button: Button

    init {
        button = jda.button(
            label = buttonInfo.label,
            style = buttonInfo.style,
            emoji = buttonInfo.emoji,
            user = user,
            disabled = !enabled
        ) {
            handle(it)
        }
    }

    abstract suspend fun handle(event: ButtonInteractionEvent)
}