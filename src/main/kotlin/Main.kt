package se.zensum.leia

import leia.bootstrap

fun main() {
    bootstrap()
}

fun getEnv(e : String, default: String? = null) : String = System.getenv()[e] ?: default ?: throw RuntimeException("Missing environment variable $e and no default value is given.")