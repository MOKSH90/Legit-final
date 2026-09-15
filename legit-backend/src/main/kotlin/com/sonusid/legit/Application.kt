package com.sonusid.legit

import com.sonusid.legit.db.MongoDB
import com.sonusid.legit.gateway.ApiGateway
import com.sonusid.legit.pipeline.DataPipelineService
import com.sonusid.legit.plugins.configureMonitoring
import com.sonusid.legit.plugins.configureSecurity
import com.sonusid.legit.plugins.configureSerialization
import com.sonusid.legit.plugins.configureStatusPages
import com.sonusid.legit.services.DocumentService
import com.sonusid.legit.services.FirebaseService
import com.sonusid.legit.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.coroutines.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // ========================
    // 0. READ CONFIGURATION
    // Pull secrets and config from env / application.yaml
    // ========================
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: "legit-super-secret-key-change-this-in-production-minimum-256-bits"

    val jwtIssuer = System.getenv("JWT_ISSUER")
        ?: environment.config.propertyOrNull("jwt.issuer")?.getString()
        ?: "legit-platform"

    val jwtAudience = System.getenv("JWT_AUDIENCE")
        ?: environment.config.propertyOrNull("jwt.audience")?.getString()
        ?: "legit-users"

    val jwtRealm = environment.config.propertyOrNull("jwt.realm")?.getString() ?: "legit"

    val jwtExpirationMs = environment.config
        .propertyOrNull("jwt.expirationMs")?.getString()?.toLongOrNull()
        ?: 3600000L // 1 hour

    val refreshExpirationMs = environment.config
        .propertyOrNull("jwt.refreshExpirationMs")?.getString()?.toLongOrNull()
        ?: 604800000L // 7 days

    val encryptionSecret = System.getenv("LEGIT_ENCRYPTION_SECRET")
        ?: environment.config.propertyOrNull("legit.encryptionSecret")?.getString()
        ?: "legit-document-encryption-secret-change-in-production-64chars-min"

    val pipelineSecret = System.getenv("LEGIT_PIPELINE_SECRET")
        ?: environment.config.propertyOrNull("legit.pipelineSecret")?.getString()
        ?: "legit-pipeline-verification-secret-change-in-production"

    val disposableKeyTtlMs = environment.config
        .propertyOrNull("legit.disposableKeyTtlMs")?.getString()?.toLongOrNull()
        ?: (5 * 60 * 1000L) // 5 minutes

    // ========================
    // 1. FIREBASE INITIALIZATION
    // ========================
    FirebaseService.init()

    // ========================
    // 2. CORE PLUGINS
    // ========================
    configureSerialization()
    configureStatusPages()
    configureSecurity(jwtSecret, jwtIssuer, jwtAudience, jwtRealm)
    configureMonitoring()
    configureCORS()

    // ========================
    // 3. DATABASE
    // ========================
    MongoDB.init(this)

    // ========================
    // 4. INITIALIZE SERVICES
    // Wire up all service instances with their dependencies
    // ========================
    val userService = UserService(
        jwtSecret = jwtSecret,
        jwtIssuer = jwtIssuer,
        jwtAudience = jwtAudience,
        jwtExpirationMs = jwtExpirationMs,
        refreshExpirationMs = refreshExpirationMs
    )

    runBlocking {
        try {
            userService.seedDefaultAccounts()
        } catch (e: Exception) {
            log.warn("Account seeding skipped: ${e.message}")
        }
    }

    val documentService = DocumentService(
        encryptionSecret = encryptionSecret
    )

    val pipelineService = DataPipelineService(
        documentService = documentService,
        userService = userService,
        pipelineSecret = pipelineSecret,
        jwtSecret = jwtSecret,
        jwtIssuer = jwtIssuer,
        disposableKeyTtlMs = disposableKeyTtlMs
    )

    // ========================
    // 5. API GATEWAY
    // Single entry point that aggregates all service routes
    // ========================
    val gateway = ApiGateway(
        userService = userService,
        documentService = documentService,
        pipelineService = pipelineService
    )

    with(gateway) {
        configureGateway()
    }

    // ========================
    // 6. BACKGROUND TASKS
    // Periodic cleanup of expired contracts and burned keys
    // ========================
    val cleanupScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    cleanupScope.launch {
        while (isActive) {
            try {
                delay(15 * 60 * 1000L) // Every 15 minutes
                val cleaned = pipelineService.cleanupExpiredContracts()
                if (cleaned > 0) {
                    log.info("Scheduled cleanup: $cleaned expired contracts processed")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Scheduled cleanup failed", e)
            }
        }
    }

    // Cancel cleanup job when application stops
    monitor.subscribe(ApplicationStopped) {
        cleanupScope.cancel()
    }

    // ========================
    // STARTUP LOG
    // ========================
    log.info("=".repeat(60))
    log.info("  LEGIT — Secure Data Verification Pipeline")
    log.info("  Version: ${ApiGateway.VERSION}")
    log.info("  Port: ${environment.config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8080"}")
    log.info("  ")
    log.info("  Services:")
    log.info("    ✓ UserService        — JWT Auth, Sessions, User Management")
    log.info("    ✓ DocumentService    — Encrypted Document Vault (AES-256-GCM)")
    log.info("    ✓ DataPipeline       — Contractual Verification with Disposable Keys")
    log.info("    ✓ API Gateway        — Route Aggregation & Health Monitoring")
    log.info("  ")
    log.info("  Your documents never leave. Only verification does.")
    log.info("=".repeat(60))
}

/**
 * Configure CORS for cross-origin requests.
 * In production, restrict this to your actual frontend domains.
 */
fun Application.configureCORS() {
    install(CORS) {
        // HTTP Methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        // Headers
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.AccessControlRequestMethod)
        allowHeader(HttpHeaders.AccessControlRequestHeaders)
        allowHeader("X-Requested-With")

        // Allow credentials (cookies/sessions)
        allowCredentials = true
        allowNonSimpleContentTypes = true

        // Development policy: reflect common local and tunnel origins.
        allowOrigins { origin ->
            origin.startsWith("http://localhost:") ||
                origin.startsWith("https://localhost:") ||
                origin.startsWith("http://127.0.0.1:") ||
                origin.startsWith("https://127.0.0.1:") ||
                origin.endsWith(".ngrok-free.app") ||
                origin.endsWith(".ngrok.app")
        }

        // Max age for preflight cache
        maxAgeInSeconds = 3600
    }
}
