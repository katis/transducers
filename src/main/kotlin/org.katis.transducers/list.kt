package org.katis.transducers

@Suppress("unchecked_cast")
val <T> ListReducer: ReducingFunction<List<T>, T> get() = listReducerPriv as ReducingFunction<List<T>, T>

private val listReducerPriv: ReducingFunction<List<Any?>, Any?> = ListReducerO()

private class ListReducerO<T> : ReducingFunction<List<T>, T> {
    override fun init(): List<T> = arrayListOf()

    override fun complete(result: List<T>): List<T> {
        return result
    }

    override fun step(result: List<T>, input: T, reduced: Reduced): List<T> {
        return when (result) {
            is MutableList<T> -> {
                result.add(input)
                result
            }
            else -> result + input
        }
    }
}

fun <A, B> Reducable<B>.toList(trf: (Transducer<B, B>) -> Transducer<A, B>): List<A> {
    return this.transduce(ListReducer, trf)
}