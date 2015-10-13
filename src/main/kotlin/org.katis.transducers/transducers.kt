package org.katis.transducers

import java.util.concurrent.atomic.AtomicBoolean

class Reduced() {
    private val _value: AtomicBoolean = AtomicBoolean(false)
    val value: Boolean get() = _value.get()

    fun set(): Unit = _value.set(true)
}

interface StepFunction<R, T> {
    fun step(result: R, input: T, reduced: Reduced): R
}

interface ReducingFunction<R, T> : StepFunction<R, T> {
    fun init(): R {
        throw IllegalStateException()
    }
    fun complete(result: R): R {
        return result
    }
}

abstract class ChainReducingFunction<R, A, B>(
        protected val rf: ReducingFunction<R, A>
) : ReducingFunction<R, B> {
    override fun init(): R = rf.init()

    override fun complete(result: R): R = rf.complete(result)
}

interface Transducer<B, C> {
    operator fun <R> invoke(rf: ReducingFunction<R, B>): ReducingFunction<R, C>

    fun <A> compose(right: Transducer<A, B>): Transducer<A, C> {
        return object : Transducer<A, C> {
            override fun <R> invoke(rf: ReducingFunction<R, A>): ReducingFunction<R, C> {
                return this@Transducer(right(rf))
            }
        }
    }
}

interface Reducable<B> {
    fun <R> reduce(rf: ReducingFunction<R, B>, result: R, reduced: Reduced = Reduced()): R

    fun <R, A> transduce(rf: ReducingFunction<R, A>, tr: Transducer<A, B>): R {
        val _xf = tr(rf)
        return reduce(_xf, rf.init())
    }

    fun <R, A> transduce(rf: ReducingFunction<R, A>, trf: (Transducer<B, B>) -> Transducer<A, B>): R {
        return transduce(rf, trf(noOp<B>()))
    }
}
