package tech.goksi.pterobot.events.handlers

import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager
import com.mattmalec.pterodactyl4j.client.ws.events.AuthSuccessEvent
import com.mattmalec.pterodactyl4j.client.ws.events.Event
import com.mattmalec.pterodactyl4j.client.ws.events.output.ConsoleOutputEvent
import com.mattmalec.pterodactyl4j.client.ws.hooks.ClientSocketListener
import com.mattmalec.pterodactyl4j.client.ws.hooks.IClientListenerManager
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.internal.toImmutableList
import tech.goksi.pterobot.util.Common
import java.lang.StringBuilder
import java.util.concurrent.CopyOnWriteArrayList

class PterodactylEventManager : IClientListenerManager {
    private val logger by SLF4J
    private val scope = Common.getDefaultCoroutineScope()
    private val listeners = CopyOnWriteArrayList<Any>()
    override fun register(listener: Any) {
        listeners.add(
            when (listener) {
                is PterodactylEventListener, is ClientSocketListener -> listener
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
                if (listener is PterodactylEventListener) {
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

    inline fun websocketListener(crossinline consumer: suspend PterodactylEventListener.(ConsoleOutputEvent) -> Unit): PterodactylEventListener {
        return object : PterodactylEventListener {
            override suspend fun onEvent(event: Event) {
                if (event is AuthSuccessEvent) event.webSocketManager.request(WebSocketManager.RequestAction.LOGS)
                else if (event is ConsoleOutputEvent) consumer(event)
            }

            override fun cancel() = unregister(this)
        }.also { register(it) }
    }
}

suspend fun ClientServer.getLogs(): String {
    val stringJoiner = StringBuilder()
    val handler = PterodactylEventManager()
    val task = handler.websocketListener { stringJoiner.append(it.line).append('\n'); }
    val wss = this.webSocketBuilder.addEventListeners(task).build()
    delay(task.expireAfter)
    task.cancel()
    wss.shutdown()
    return stringJoiner.toString()
}
