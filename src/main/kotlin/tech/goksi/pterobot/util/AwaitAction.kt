package tech.goksi.pterobot.util

import com.mattmalec.pterodactyl4j.PteroAction
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> CompletableFuture<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation { cancel(true) }
    whenComplete { result, exception ->
        if (exception != null) it.resumeWithException(exception)
        else it.resume(result)
    }
}

suspend fun <T> PteroAction<T>.await(): T = submit().await()
