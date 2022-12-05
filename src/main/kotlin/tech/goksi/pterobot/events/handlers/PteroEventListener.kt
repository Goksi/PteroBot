package tech.goksi.pterobot.events.handlers

import com.mattmalec.pterodactyl4j.client.ws.events.Event
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface PteroEventListener {
    val expireAfter: Duration get() = 2.seconds // 2 seconds fixed for expiration

    suspend fun onEvent(event: Event)

    fun cancel()
}
