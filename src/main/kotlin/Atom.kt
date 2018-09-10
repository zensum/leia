package leia

import java.util.concurrent.atomic.AtomicReference

/**
 * An atom wraps provides a single element of state. It does this by providing
 * an [AtomicReference] to an immutable value.
 */
interface Atom<T> {
    val reference: AtomicReference<T>
}