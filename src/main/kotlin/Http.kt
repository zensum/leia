package se.zensum.leia

import io.ktor.http.HttpMethod

object HttpMethods {
    val verbs: Set<HttpMethod> = setOf(
        HttpMethod.Get,
        HttpMethod.Post,
        HttpMethod.Put,
        HttpMethod.Patch,
        HttpMethod.Delete,
        HttpMethod.Head,
        HttpMethod.Options
    )

    operator fun contains(method: HttpMethod): Boolean = method in verbs
    operator fun contains(method: String): Boolean = HttpMethod.parse(method.toUpperCase()) in verbs
}