package org.katis.transducers

class ArrayReducable<T>(val array: Array<T>) : Reducable<T> {
    override fun <R> reduce(rf: ReducingFunction<R, T>, result: R, reduced: Reduced): R {
        var ret = result
        loop@ for (t in array) {
            val r = rf.step(ret, t, reduced)
            ret = r
            if (reduced.value) {
                break@loop
            }
        }
        return rf.complete(ret)
    }
}

val <T> Array<T>.tx: Reducable<T> get() = ArrayReducable(this)
