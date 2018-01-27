package adeln

import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.Response
import org.jetbrains.ktor.response.ApplicationResponse
import org.jetbrains.ktor.response.header
import java.io.IOException

suspend fun Call.await(): Response =
    suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException): Unit =
                cont.resumeWithException(e)

            override fun onResponse(call: Call?, response: Response): Unit =
                cont.resume(response)
        })
    }

var ApplicationResponse.okHeaders: Headers
    get() = TODO()
    set(value) =
        repeat(value.size()) { idx ->
            header(value.name(idx), value.value(idx))
        }
