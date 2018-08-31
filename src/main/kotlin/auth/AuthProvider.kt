package se.zensum.leia.auth

import leia.logic.IncomingRequest

interface AuthProvider {
    fun verify(matching: List<String>, incomingRequest: IncomingRequest) : AuthResult
}