package com.hlk.rest_api_demo.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*
import java.util.function.Function
import kotlin.collections.HashMap

@Component
class JwtTokenUtil : Serializable {
    @Value("\${jwt.secret}")
    private val secret: String? = null

    @Value("\${jwt.expireTimeInHour}")
    private val expireTimeInHour: Long = 5


    fun getUsernameFromToken(token: String?): String {
        return getClaimFromToken(token) { obj: Claims -> obj.subject }
    }

    fun getIssuedAtDateFromToken(token: String?): Date {
        return getClaimFromToken(token) { obj: Claims -> obj.issuedAt }
    }

    fun getExpirationDateFromToken(token: String?): Date {
        return getClaimFromToken(token) { obj: Claims -> obj.expiration }
    }

    fun <T> getClaimFromToken(token: String?, claimsResolver: Function<Claims, T>): T {
        val claims = getAllClaimsFromToken(token)
        return claimsResolver.apply(claims)
    }

    private fun getAllClaimsFromToken(token: String?): Claims {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).body
    }

    private fun isTokenExpired(token: String): Boolean {
        val expiration = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }

    private fun ignoreTokenExpiration(token: String): Boolean {
        // here you specify tokens, for that the expiration is ignored
        return false
    }

    fun generateToken(userDetails: UserDetails): String {
        val claims: Map<String, Any?> = HashMap()
        return doGenerateToken(claims, userDetails.username)
    }

    private fun doGenerateToken(claims: Map<String, Any?>, subject: String): String {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + expirationTimeInHour()))
            .signWith(SignatureAlgorithm.HS256, secret).compact()
    }

    fun canTokenBeRefreshed(token: String): Boolean {
        return !isTokenExpired(token) || ignoreTokenExpiration(token)
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = getUsernameFromToken(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    fun expirationTimeInHour(): Long {
        return expireTimeInHour * 60 * 60 * 1000
    }
}
