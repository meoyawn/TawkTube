package adeln

tailrec fun <T, P> paging(pageToken: P? = null,
                          accum: MutableList<T> = mutableListOf(),
                          nextPage: (T) -> P?,
                          load: (page: P?) -> T): List<T> {

    val x = load(pageToken)

    accum += x

    val np = nextPage(x)

    return when (np) {
        null -> accum
        else -> paging(pageToken = np, accum = accum, nextPage = nextPage, load = load)
    }
}
