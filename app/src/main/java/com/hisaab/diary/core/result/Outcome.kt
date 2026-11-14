package com.hisaab.diary.core.result

/** Result of an action that can fail with a known, expected [E] error (like bad input). */
sealed interface Outcome<out T, out E> {
    data class Success<T>(val value: T) : Outcome<T, Nothing>

    data class Failure<E>(val error: E) : Outcome<Nothing, E>
}

inline fun <T, E> Outcome<T, E>.onSuccess(block: (T) -> Unit): Outcome<T, E> {
    if (this is Outcome.Success) block(value)
    return this
}

inline fun <T, E> Outcome<T, E>.onFailure(block: (E) -> Unit): Outcome<T, E> {
    if (this is Outcome.Failure) block(error)
    return this
}
