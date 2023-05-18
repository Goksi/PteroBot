package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroAction
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.SubscribeEvent
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

suspend inline fun <T> suspendWithTimeout(
    timeout: Duration,
    crossinline block: (CancellableContinuation<T>) -> Unit
): T? {
    var result: T? = null
    withTimeoutOrNull(timeout) {
        result = suspendCancellableCoroutine(block = block)
    }
    return result
}

suspend fun <T> CompletableFuture<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation { cancel(true) }
    whenComplete { result, exception ->
        if (exception != null) it.resumeWithException(exception)
        else it.resume(result)
    }
}

suspend fun <T> PteroAction<T>.await(): T = submit().await()

suspend inline fun <reified T : GenericEvent> JDA.awaitEvent(
    timeout: Duration = 5.minutes,
    crossinline filter: (T) -> Boolean
) = suspendWithTimeout(timeout) {
    val listener = object : EventListener {
        @SubscribeEvent
        override fun onEvent(event: GenericEvent) {
            if (event is T && filter(event)) {
                removeEventListener(this)
                it.resume(event)
            }
        }
    }
    addEventListener(listener)
    it.invokeOnCancellation { removeEventListener(listener) }
}
