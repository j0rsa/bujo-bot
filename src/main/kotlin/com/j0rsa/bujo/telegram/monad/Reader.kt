package com.j0rsa.bujo.telegram.monad

/**
 * @author red
 * @since 08.02.20
 */

class Reader<D, out A>(val run: (D) -> A) {

    inline fun <B> map(crossinline fa: (A) -> B): Reader<D, B> = Reader { d ->
        fa(run(d))
    }

    inline fun <B> flatMap(crossinline fa: (A) -> Reader<D, B>): Reader<D, B> = Reader { d ->
        fa(run(d)).run(d)
    }

    companion object {
        fun <D, A> just(a: A): Reader<D, A> = Reader { a }
        fun <D> ask(): Reader<D, D> = Reader { it }
    }
}
