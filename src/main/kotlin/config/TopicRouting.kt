package se.zensum.leia.config

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

enum class Format { RAW_BODY, PROTOBUF }

data class TopicRouting(val path: String,
                        val topic: String,
                        val format: Format = Format.PROTOBUF,
                        val verify: Boolean = false,
                        val allowedMethods: Collection<HttpMethod>,
                        val corsHosts: List<String>,
                        val response: HttpStatusCode,
                        val sink: String? = null)

