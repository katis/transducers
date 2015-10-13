package org.katis.transducers

class IterableReducable<T>(val self: Iterable<T>) : Reducable<T> {
    override fun <R> reduce(rf: ReducingFunction<R, T>, result: R, reduced: Reduced): R {
        var ret = result
        loop@ for (t in self) {
            val r = rf.step(ret, t, reduced)
            ret = r
            if (reduced.value) {
                break@loop
            }
        }
        return rf.complete(ret)
    }
}

val <T> Iterable<T>.tx: Reducable<T> get() = IterableReducable(this)
