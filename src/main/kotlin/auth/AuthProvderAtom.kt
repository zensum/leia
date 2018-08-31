package se.zensum.leia.auth

import leia.Atom
import leia.logic.IncomingRequest
import se.zensum.leia.auth.AuthProvider

class AuthProviderAtom(private var currentProvider: AuthProvider): AuthProvider, Atom<AuthProvider> {
    override fun set(new: AuthProvider) {
        currentProvider = new
    }

    override fun verify(matching: List<String>, incomingRequest: IncomingRequest) : AuthResult
        = currentProvider.verify(matching, incomingRequest)
}