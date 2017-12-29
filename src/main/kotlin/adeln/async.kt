package adeln

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import java.util.concurrent.Executors
import kotlin.coroutines.experimental.CoroutineContext

val BLOCKING_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

suspend fun <Initial, Element, Additional, Result> mapReduce(
    initial: () -> Initial,
    extractN: (Initial) -> List<Element>,
    map: (Element) -> Additional,
    reduce: (Initial, List<Element>, List<Additional>) -> Result
): Result {
    val start = initial()

    val elements = extractN(start)

    return reduce(start, elements, elements.parMap(BLOCKING_IO, map))
}

suspend fun <T, R> Iterable<T>.parMap(dispatcher: CoroutineContext, transform: (T) -> R): List<R> =
    map { el -> async(dispatcher) { transform(el) } }.map { it.await() }

data class AsyncOp<out T>(
    val pool: CoroutineContext,
    val work: () -> T
)

operator fun <T> AsyncOp<T>.invoke(): Deferred<T> =
    async(pool) {
        work()
    }
