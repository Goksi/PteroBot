package tech.goksi.pterobot.events

import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import tech.goksi.pterobot.commands.NodeInfo
import java.util.Timer

class NodeStatusDelete: ListenerAdapter() {

    override fun onMessageDelete(event: MessageDeleteEvent) {
        val timer: Timer? = NodeInfo.mapping[event.messageIdLong]
        if(timer != null){
            NodeInfo.mapping.remove(event.messageIdLong)
            timer.cancel()
            timer.purge()
        }
    }
}