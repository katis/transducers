package org.katis.transducers

import java.util.*

private val noOpTx = object : Transducer<Any?, Any?> {
        override fun <R> invoke(rf: ReducingFunction<R, Any?>): ReducingFunction<R, Any?> {
            return rf
        }
    }

@Suppress("unchecked_cast")
fun <A> noOp(): Transducer<A, A> = noOpTx as Transducer<A, A>

fun <A, B> map(f: (B) -> A): Transducer<A, B> {
    return object : Transducer<A, B> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, B> {
            return object : ChainReducingFunction<R, A, B>(rf) {
                override fun step(result: R, input: B, reduced: Reduced): R {
                    return rf.step(result, f(input), reduced)
                }
            }
        }
    }
}

fun <A: Any, B, C> Transducer<B, C>.mapAny(f: (B) -> A?): Transducer<A, C> {
     return this.compose(object : Transducer<A, B> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, B> {
            return object : ChainReducingFunction<R, A, B>(rf) {
                override fun step(result: R, input: B, reduced: Reduced): R {
                    val v = f(input) ?: return result
                    return rf.step(result, v, reduced)
                }
            }
        }
    })
}

fun <A, B, C> Transducer<B, C>.map(transformer: (B) -> A): Transducer<A, C> {
    return this.compose(map(f = transformer))
}

fun <A> filter(f: (A) -> Boolean): Transducer<A, A> {
    return object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> {
            return object : ChainReducingFunction<R, A, A>(rf) {
                override fun step(result: R, input: A, reduced: Reduced): R {
                    if (f(input)) return rf.step(result, input, reduced)
                    return result
                }
            }
        }
    }
}

fun <A, B> Transducer<A, B>.filter(predicate: (A) -> Boolean): Transducer<A, B> {
    return this.compose(filter(f = predicate))
}

fun <A> takeWhile(f: (A) -> Boolean): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                override fun step(result: R, input: A, reduced: Reduced): R =
                    if (f(input)) {
                        rf.step(result, input, reduced)
                    } else {
                        reduced.set()
                        result
                    }
            }
    }

fun <A, B> Transducer<A, B>.takeWhile(predicate: (A) -> Boolean): Transducer<A, B> {
    return this.compose(takeWhile(f = predicate))
}

fun <A, B : Reducable<A>> cat(): Transducer<A, B> =
    object : Transducer<A, B> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, B> =
            object : ChainReducingFunction<R, A, B>(rf) {
                override fun step(result: R, input: B, reduced: Reduced): R =
                    input.reduce(rf, result, reduced)
            }
    }

fun <A, B : Reducable<A>, C> Transducer<B, C>.cat(): Transducer<A, C> {
    return this.compose(org.katis.transducers.cat())
}

fun <A, B : Reducable<A>, C> mapcat(f: (C) -> B): Transducer<A, C> = map(f).compose(cat())

fun <A, B : Reducable<A>, C, D> Transducer<C, D>.mapcat(f: (C) -> B): Transducer<A, D> {
    return compose(org.katis.transducers.mapcat(f))
}

fun <A> take(n: Long): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                var taken: Long = 0
                override fun step(result: R, input: A, reduced: Reduced): R =
                    if (taken < n) {
                        taken++
                        rf.step(result, input, reduced)
                    } else {
                        reduced.set()
                        result
                    }
            }
    }

fun <A, B> Transducer<A, B>.take(n: Long): Transducer<A, B> = compose(org.katis.transducers.take(n))

fun <A> drop(n: Long): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                var dropped = 0
                override fun step(result: R, input: A, reduced: Reduced): R =
                    if (dropped < n) {
                        dropped++
                        result
                    } else {
                        rf.step(result, input, reduced)
                    }
            }
}
fun <A, B> Transducer<A, B>.drop(n: Long): Transducer<A, B> {
    return compose(org.katis.transducers.drop(n))
}

fun <A> dropWhile(f: (A) -> Boolean): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                var drop = true
                override fun step(result: R, input: A, reduced: Reduced): R =
                    if (drop && f(input)) {
                        result
                    } else {
                        drop = false
                        rf.step(result, input, reduced)
                    }
            }
    }

fun <A, B> Transducer<A, B>.dropWhile(predicate: (A) -> Boolean): Transducer<A, B> {
    return compose(org.katis.transducers.dropWhile(predicate))
}

fun <A> replace(replacements: Map<A, A>): Transducer<A, A> {
    return map { replacements.getOrElse(it) { it } }
}

fun <A, B> Transducer<A, B>.replace(replacements: Map<A, A>): Transducer<A, B> {
    return compose(org.katis.transducers.replace(replacements))
}

fun <A: Any> keep(f: (A) -> A?): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                override fun step(result: R, input: A, reduced: Reduced): R {
                    val a = f(input)
                    return if (a == null) result else rf.step(result, a, reduced)
                }
            }
    }

fun <A: Any, B> Transducer<A, B>.keep(fn: (A) -> A?): Transducer<A, B> {
    return compose(org.katis.transducers.keep(fn))
}

fun <A, P> partitionBy(f: (A) -> P): Transducer<Iterable<A>, A> {
    return object : Transducer<Iterable<A>, A> {
        override fun <R> invoke(rf: ReducingFunction<R, Iterable<A>>): ReducingFunction<R, A> {
            return object : ReducingFunction<R, A> {
                val part: MutableCollection<A> = arrayListOf()
                var mark: Any? = Object()
                var prior: Any? = Object()

                override fun init(): R = rf.init()

                override fun complete(result: R): R {
                    return if (part.isEmpty()) {
                        rf.complete(result)
                    } else {
                        val copy = ArrayList(part)
                        part.clear()
                        rf.step(result, copy, Reduced())
                    }
                }

                override fun step(result: R, input: A, reduced: Reduced): R {
                    val v = f(input)
                    if ((prior === mark) || prior!!.equals(v)) {
                        prior = v
                        part.add(input)
                        return result
                    } else {
                        val copy = ArrayList(part)
                        prior = v
                        part.clear()
                        val ret = rf.step(result, copy, reduced)
                        if (!reduced.value) {
                            part.add(input)
                        }
                        return ret
                    }
                }
            }
        }
    }
}

fun <A, B, P> Transducer<A, B>.partitionBy(f: (A) -> P): Transducer<Iterable<A>, B> {
    return compose(org.katis.transducers.partitionBy(f))
}

fun <A: Any> tap(f: (Any?, A, Reduced) -> Unit): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                override fun step(result: R, input: A, reduced: Reduced): R {
                    f(result, input, reduced)
                    return rf.step(result, input, reduced)
                }
            }
    }

fun <A: Any, B> Transducer<A, B>.tap(fn: (Any?, A, Reduced) -> Unit): Transducer<A, B> {
    return compose(org.katis.transducers.tap(fn))
}

fun <A: Any> tapItem(f: (A) -> Unit): Transducer<A, A> =
    object : Transducer<A, A> {
        override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, A> =
            object : ChainReducingFunction<R, A, A>(rf) {
                override fun step(result: R, input: A, reduced: Reduced): R {
                    f(input)
                    return rf.step(result, input, reduced)
                }
            }
    }

fun <A: Any, B> Transducer<A, B>.tapItem(fn: (A) -> Unit): Transducer<A, B> {
    return compose(org.katis.transducers.tapItem(fn))
}

internal data class Entry<K, V>(private val ekey: K, private val evalue: V) : Map.Entry<K, V> {
    override fun getKey(): K = ekey

    override fun getValue(): V = evalue
}

fun <C, K, V, KA> Transducer<Map.Entry<K, V>, C>.mapKeys(f: (Map.Entry<K, V>) -> KA): Transducer<Map.Entry<KA, V>, C> {
    return this.compose(
        object : Transducer<Map.Entry<KA, V>, Map.Entry<K, V>> {
            override fun <R> invoke(rf: ReducingFunction<R, Map.Entry<KA, V>>): ReducingFunction<R, Map.Entry<K, V>> {
                return object : ChainReducingFunction<R, Map.Entry<KA, V>, Map.Entry<K, V>>(rf) {
                    override fun step(result: R, input: Map.Entry<K, V>, reduced: Reduced): R {
                        return rf.step(result, Entry(f(input), input.value), reduced)
                    }
                }
            }
        }
    )
}

