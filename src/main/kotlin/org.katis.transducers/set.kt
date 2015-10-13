package org.katis.transducers

class SetReducable<T>(var set: Set<T>) : Reducable<T> {
    override fun <R> reduce(rf: ReducingFunction<R, T>, result: R, reduced: Reduced): R {
        var ret = result
        loop@ for (v in set) {
            val r = rf.step(ret, v, reduced)
            ret = r
            if (reduced.value) {
                break@loop
            }
        }
        return rf.complete(ret)
    }
}

val <T> Set<T>.tx: Reducable<T> get() = SetReducable(this)

@Suppress("unchecked_cast")
val <T> SetReducer: ReducingFunction<Set<T>, T> get() = SetReducer_ as ReducingFunction<Set<T>, T>
private val SetReducer_: ReducingFunction<Set<Any?>, Any?> = SetReducerO()

private class SetReducerO<T>() : ReducingFunction<Set<T>, T> {
    override fun init(): Set<T> = hashSetOf()

    override fun complete(result: Set<T>): Set<T> {
        return result
    }

    override fun step(result: Set<T>, input: T, reduced: Reduced): Set<T> {
        return when (result) {
            is MutableSet<T> -> {
                result.add(input)
                result
            }
            else -> result + input
        }
    }
}

fun <A, B> Reducable<B>.toSet(trf: (Transducer<B, B>) -> Transducer<A, B>): Set<A> {
    return this.transduce(SetReducer, trf)
}