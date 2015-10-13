package org.katis.transducers

class StringReducable(val str: String) : Reducable<Char> {
    override fun <R> reduce(rf: ReducingFunction<R, Char>, result: R, reduced: Reduced): R {
        var ret = result
        loop@ for (t in str) {
            val r = rf.step(ret, t, reduced)
            ret = r
            if (reduced.value) {
                break@loop
            }
        }
        return rf.complete(ret)
    }
}

val String.tx: Reducable<Char> get() = StringReducable(this)

private val stringBufferReducerVal = StringBufferReducerO<Any?>()

@Suppress("unchecked_cast")
val <T> StringBufferReducer: ReducingFunction<StringBuffer, T>
        = stringBufferReducerVal as ReducingFunction<StringBuffer, T>

private class StringBufferReducerO<T> : ReducingFunction<StringBuffer, T> {
    override fun init(): StringBuffer = StringBuffer()

    override fun complete(result: StringBuffer): StringBuffer {
        return result
    }

    override fun step(result: StringBuffer, input: T, reduced: Reduced): StringBuffer {
        when (input) {
            is Char -> result.append(input)
            is String -> result.append(input)
            is StringBuffer -> result.append(input)
            is Boolean -> result.append(input)
            is CharSequence -> result.append(input)
            is CharArray -> result.append(input)
            is Int -> result.append(input)
            is Long -> result.append(input)
            is Double -> result.append(input)
            is Float -> result.append(input)
            else -> result.append(input)
        }
        return result
    }
}

fun <A, B> Reducable<B>.toStringBuffer(trf: (Transducer<B, B>) -> Transducer<A, B>): StringBuffer {
    return this.transduce(StringBufferReducer, trf)
}