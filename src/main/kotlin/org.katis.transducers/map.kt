package org.katis.transducers

class MapReducable<K, V>(var self: Map<K, V>) : Reducable<Map.Entry<K, V>> {
    override fun <R> reduce(rf: ReducingFunction<R, Map.Entry<K, V>>, result: R, reduced: Reduced): R {
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

val <K, V> Map<K, V>.tx: Reducable<Map.Entry<K, V>> get() = MapReducable(this)

class MapReducer<K, V>() : ReducingFunction<Map<K, V>, Map.Entry<K, V>> {
    override fun init(): Map<K, V> = hashMapOf()

    override fun complete(result: Map<K, V>): Map<K, V> {
        return result
    }

    override fun step(result: Map<K, V>, input: Map.Entry<K, V>, reduced: Reduced): Map<K, V> {
        return when (result) {
            is MutableMap<K, V> -> {
                result.put(input.key, input.value)
                result
            }
            else -> result + Pair(input.key, input.value)
        }
    }
}

fun <B, K, V> Reducable<B>.toMap(trf: (Transducer<B, B>) -> Transducer<Pair<K, V>, B>): Map<K, V> {
    return this.transduce(MapReducer(), trf(noOp()).map { Entry(it.first, it.second) as Map.Entry<K, V> })
}


