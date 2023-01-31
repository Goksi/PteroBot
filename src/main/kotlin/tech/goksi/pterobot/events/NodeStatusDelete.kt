package tech.goksi.pterobot.events

import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.Job
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import tech.goksi.pterobot.commands.NodeCommand

class NodeStatusDelete : ListenerAdapter() {
    private val logger by SLF4J

    override fun onMessageDelete(event: MessageDeleteEvent) {
        val job: Job? = NodeCommand.taskMap[event.messageIdLong]
        if (job != null) {
            NodeCommand.taskMap.remove(event.messageIdLong)
            logger.debug("Removed ${event.messageId} node info mapper")
            job.cancel()
        }
    }
}
