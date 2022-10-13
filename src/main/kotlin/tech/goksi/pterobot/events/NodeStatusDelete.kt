package tech.goksi.pterobot.events

import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import tech.goksi.pterobot.commands.NodeInfo
import java.util.Timer

class NodeStatusDelete: ListenerAdapter() {
    private val logger by SLF4J

    override fun onMessageDelete(event: MessageDeleteEvent) {
        val timer: Timer? = NodeInfo.mapping[event.messageIdLong]
        if(timer != null){
            NodeInfo.mapping.remove(event.messageIdLong)
            logger.debug("Removed ${event.messageId} node info mapper")
            timer.cancel()
            timer.purge()
        }
    }
}