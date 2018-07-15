package leia

// An atom wraps provides a single element of state. It does this by providing
// mutable wrapper point to an immutable value.
interface Atom<in T> {
    // Sets the value of the atom to the passed in value.
    fun set(new: T): Unit
}