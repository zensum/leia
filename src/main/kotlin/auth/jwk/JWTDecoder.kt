package se.zensum.leia.auth.jwk

import com.auth0.jwt.interfaces.DecodedJWT

interface JWTDecoder {
    fun verifyToken(token: String): DecodedJWT?
}