package adeln

tailrec fun <T, P> paging(pageToken: P? = null,
                          accum: MutableList<T> = mutableListOf(),
                          nextPage: (T) -> P?,
                          load: (page: P?) -> T,
                          limit: Int): List<T> {

    val x = load(pageToken)

    accum += x

    val np = nextPage(x)

    return when {
        np == null || limit == 0 ->
            accum

        else ->
            paging(pageToken = np, accum = accum, nextPage = nextPage, load = load, limit = limit - 1)
    }
}
