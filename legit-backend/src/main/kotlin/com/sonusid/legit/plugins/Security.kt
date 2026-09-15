package com.sonusid.legit.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sonusid.legit.models.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

fun Application.configureSecurity(
    jwtSecret: String = System.getenv("JWT_SECRET") ?: "legit-super-secret-key-change-this-in-production-minimum-256-bits",
    jwtIssuer: String = System.getenv("JWT_ISSUER") ?: "legit-platform",
    jwtAudience: String = System.getenv("JWT_AUDIENCE") ?: "legit-users",
    jwtRealm: String = "legit"
) {

    install(Sessions) {
        cookie<UserSession>("LEGIT_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400 // 24 hours
            cookie.httpOnly = true
            cookie.secure = false // Set to true in production with HTTPS
            cookie.extensions["SameSite"] = "lax"
        }
    }

    authentication {
        // Primary JWT auth for regular users
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .build()
            )
            authHeader { call ->
                call.request.parseAuthorizationHeader() ?: call.request.queryParameters["token"]?.let {
                    HttpAuthHeader.Single("Bearer", it)
                }
            }
            validate { credential ->
                val userId = credential.payload.subject
                val type = credential.payload.getClaim("type")?.asString()

                if (userId != null && (type == "access" || type == null)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.error<Unit>("Authentication required. Please provide a valid JWT token.")
                )
            }
        }

        // Service provider / Forensics / Officer JWT auth — allows creating contracts
        jwt("auth-service-provider") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .build()
            )
            authHeader { call ->
                call.request.parseAuthorizationHeader() ?: call.request.queryParameters["token"]?.let {
                    HttpAuthHeader.Single("Bearer", it)
                }
            }
            validate { credential ->
                val userId = credential.payload.subject
                val role = credential.payload.getClaim("role")?.asString()
                val type = credential.payload.getClaim("type")?.asString()

                val isAuthorizedRole = role == UserRole.SERVICE_PROVIDER.name ||
                        role == UserRole.ADMIN.name ||
                        role == UserRole.FORENSICS_SPECIALIST.name ||
                        role == UserRole.POLICE_OFFICER.name ||
                        role == UserRole.LEGAL_AUTHORITY.name

                if (userId != null && (type == "access" || type == null) && isAuthorizedRole) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse.error<Unit>("Access denied. Authorized forensics, officer, or admin role required.")
                )
            }
        }

        // Admin-only JWT auth
        jwt("auth-admin") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .build()
            )
            authHeader { call ->
                call.request.parseAuthorizationHeader() ?: call.request.queryParameters["token"]?.let {
                    HttpAuthHeader.Single("Bearer", it)
                }
            }
            validate { credential ->
                val userId = credential.payload.subject
                val role = credential.payload.getClaim("role")?.asString()
                val type = credential.payload.getClaim("type")?.asString()

                if (userId != null && (type == "access" || type == null) && role == UserRole.ADMIN.name) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse.error<Unit>("Access denied. Admin role required.")
                )
            }
        }

        // Session-based auth as a fallback / secondary mechanism
        session<UserSession>("auth-session") {
            validate { session ->
                val sessionMaxAge = 24 * 60 * 60 * 1000L
                val age = System.currentTimeMillis() - session.createdAt
                if (age < sessionMaxAge) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.error<Unit>("Session expired or invalid. Please log in again.")
                )
            }
        }
    }
}

// ========================
// Extension helpers to extract user info from JWT principal
// ========================

fun JWTPrincipal.getUserId(): String = payload.subject
fun JWTPrincipal.getUsername(): String = payload.getClaim("username").asString()
fun JWTPrincipal.getLegitId(): String = payload.getClaim("legitId").asString()
fun JWTPrincipal.getRole(): UserRole = UserRole.valueOf(payload.getClaim("role").asString())
