package tech.goksi.pterobot.events.handlers

import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager
import com.mattmalec.pterodactyl4j.client.ws.events.AuthSuccessEvent
import com.mattmalec.pterodactyl4j.client.ws.events.Event
import com.mattmalec.pterodactyl4j.client.ws.events.output.OutputEvent
import com.mattmalec.pterodactyl4j.client.ws.hooks.ClientSocketListener
import com.mattmalec.pterodactyl4j.client.ws.hooks.IClientListenerManager
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.internal.toImmutableList
import tech.goksi.pterobot.util.Common
import java.util.concurrent.CopyOnWriteArrayList

class PteroEventManager : IClientListenerManager {
    private val logger by SLF4J
    private val scope = Common.getDefaultCoroutineScope()
    private val listeners = CopyOnWriteArrayList<Any>()
    override fun register(listener: Any) {
        listeners.add(
            when (listener) {
                is PteroEventListener, is ClientSocketListener -> listener
                else -> throw IllegalArgumentException("Wrong implementation of listener !")
            }
        )
    }

    override fun unregister(listener: Any) {
        listeners.remove(listener)
    }

    override fun getRegisteredListeners(): List<Any> {
        return listeners.toImmutableList()
    }

    override fun handle(event: Event) {
        scope.launch {
            for (listener in listeners) try {
                if (listener is PteroEventListener) {
                    if (listener.expireAfter.isPositive() && listener.expireAfter.isFinite()) {
                        val result = withTimeoutOrNull(listener.expireAfter.inWholeMilliseconds) {
                            listener.onEvent(event)
                        }
                        if (result == null) logger.debug("Event ${event::class.simpleName} timed out !")
                    }
                } else {
                    (listener as ClientSocketListener).onEvent(event)
                }
            } catch (exception: Exception) {
                logger.error("Uncaught exception in pterodactyl handler", exception)
            }
        }
    }

    inline fun websocketListener(crossinline consumer: (OutputEvent) -> Unit): PteroEventListener {
        return object : PteroEventListener {
            override suspend fun onEvent(event: Event) {
                if (event is AuthSuccessEvent) event.webSocketManager.request(WebSocketManager.RequestAction.LOGS)
                else if (event is OutputEvent) consumer(event)
            }

            override fun cancel() = unregister(this)
        }
    }
}
