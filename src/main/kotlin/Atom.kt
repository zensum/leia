package leia

interface Atom<in T> {
    fun set(new: T): Unit
}