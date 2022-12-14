package tech.goksi.pterobot.events.hook

import dev.minn.jda.ktx.events.CoroutineEventListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

open class CoroutineListenerAdapter : CoroutineEventListener {

    open suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {}

    open suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {}

    open suspend fun onModalInteraction(event: ModalInteractionEvent) {}

    open suspend fun onGenericEvent(event: GenericEvent) {}

    override suspend fun onEvent(event: GenericEvent) {
        onGenericEvent(event)
        when (event) {
            is SlashCommandInteractionEvent -> onSlashCommandInteraction(event)
            is StringSelectInteractionEvent -> onStringSelectInteraction(event)
            is ModalInteractionEvent -> onModalInteraction(event)
        }
    }
}
