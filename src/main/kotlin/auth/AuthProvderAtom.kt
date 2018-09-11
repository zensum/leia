package se.zensum.leia.auth

import leia.Atom
import leia.logic.IncomingRequest
import java.util.concurrent.atomic.AtomicReference

class AuthProviderAtom(currentProvider: AuthProvider): AuthProvider, Atom<AuthProvider> {
    override val reference: AtomicReference<AuthProvider> = AtomicReference(currentProvider)

    override fun verify(matching: List<String>, incomingRequest: IncomingRequest) : AuthResult
        = reference.get().verify(matching, incomingRequest)
}