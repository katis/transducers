package org.katis.transducers

class AnyReducer<T>(val predicate: (T) -> Boolean) : ReducingFunction<Boolean, T> {
    override fun init(): Boolean = false

    override fun complete(result: Boolean): Boolean = result

    override fun step(result: Boolean, input: T, reduced: Reduced): Boolean {
        if (predicate(input)) {
            reduced.set()
            return true
        }
        return false
    }
}

fun <A, B> Reducable<B>.any(predicate: (A) -> Boolean, trf: (Transducer<B, B>) -> Transducer<A, B>): Boolean {
    return this.transduce(AnyReducer(predicate), trf)
}

class NoneReducer<T>(val predicate: (T) -> Boolean) : ReducingFunction<Boolean, T> {
    override fun init(): Boolean = true

    override fun complete(result: Boolean): Boolean = result

    override fun step(result: Boolean, input: T, reduced: Reduced): Boolean {
        if (!predicate(input)) {
            reduced.set()
            return false
        }
        return true
    }
}

fun <A, B> Reducable<B>.none(predicate: (A) -> Boolean, trf: (Transducer<B, B>) -> Transducer<A, B>): Boolean {
    return this.transduce(NoneReducer(predicate), trf)
}
