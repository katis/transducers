package org.katis.transducers

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
class FirstReducer<A>(val predicate: (A) -> Boolean) : ReducingFunction<A?, A> {
    override fun init(): A? = null

    override fun complete(result: A?): A? = result

    override fun step(result: A?, input: A, reduced: Reduced): A? {
        if (predicate(input)) {
            reduced.set()
            return input
        }
        return null
    }
}

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
fun <A, B> Reducable<B>.first(predicate: (A) -> Boolean, trf: (Transducer<B, B>) -> Transducer<A, B>): A? {
    return transduce(FirstReducer(predicate), trf)
}

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
fun <A, B> Reducable<B>.first(trf: (Transducer<B, B>) -> Transducer<A, B>): A? {
    return transduce(FirstReducer<A>({ true }), trf)
}
