package com.sonusid.legit.routes

import com.sonusid.legit.models.*
import com.sonusid.legit.plugins.getUserId
import com.sonusid.legit.services.DocumentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.documentRoutes(documentService: DocumentService) {

    route("/api/v1/documents") {
        authenticate("auth-jwt", "auth-session", strategy = AuthenticationStrategy.FirstSuccessful) {

            // ========================
            // POST /api/v1/documents
            // Upload a new document to the user's vault
            // ========================
            post {
                val userId = extractDocumentUserId(call) ?: return@post

                val request = call.receive<DocumentUploadRequest>()
                val result = documentService.uploadDocument(userId, request)

                result.fold(
                    onSuccess = { document ->
                        call.respond(
                            HttpStatusCode.Created,
                            ApiResponse.success(document, "Document uploaded successfully to your vault")
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is ValidationException -> {
                                call.respond(
                                    HttpStatusCode.UnprocessableEntity,
                                    ApiError(
                                        code = error.code,
                                        message = error.message ?: "Validation failed",
                                        details = error.errors.associate { it.field to it.message }
                                    )
                                )
                            }
                            is ConflictException -> {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            else -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ApiError(
                                        code = ErrorCodes.DOCUMENT_UPLOAD_FAILED,
                                        message = error.message ?: "Failed to upload document"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            // ========================
            // GET /api/v1/documents
            // List all documents (user's or department/all based on queries/role)
            // ========================
            get {
                val userId = extractDocumentUserId(call) ?: return@get
                val allParam = call.request.queryParameters["all"]?.toBoolean() ?: false
                val deptParam = call.request.queryParameters["department"]
                val caseParam = call.request.queryParameters["caseNumber"]

                val documentList = when {
                    !caseParam.isNullOrBlank() -> {
                        val docs = documentService.getDocumentsByCaseNumber(caseParam)
                        DocumentListResponse(documents = docs, total = docs.size)
                    }
                    !deptParam.isNullOrBlank() && deptParam != "ALL" -> {
                        val docs = documentService.getDocumentsByDepartment(deptParam)
                        DocumentListResponse(documents = docs, total = docs.size)
                    }
                    allParam -> {
                        val docs = documentService.getAllEvidenceDocuments()
                        DocumentListResponse(documents = docs, total = docs.size)
                    }
                    else -> {
                        val userDocs = documentService.getDocumentsByUserId(userId).documents
                        val deptDocs = documentService.getDocumentsByDepartment("FORENSICS_TEAM")
                        val allEvidences = documentService.getAllEvidenceDocuments()
                        // If any evidence exists, combine userDocs + all evidences
                        val combined = (userDocs + deptDocs + allEvidences).distinctBy { it.id }
                        DocumentListResponse(documents = combined, total = combined.size)
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(documentList, "Documents retrieved successfully")
                )
            }

            // ========================
            // GET /api/v1/documents/{id}
            // Get a specific document by ID
            // ========================
            get("/{id}") {
                val userId = extractDocumentUserId(call) ?: return@get
                val documentId = call.parameters["id"]

                if (documentId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "Document ID is required"
                        )
                    )
                    return@get
                }

                val jwtPrincipal = call.principal<JWTPrincipal>()
                val roleStr = jwtPrincipal?.payload?.getClaim("role")?.asString()
                    ?: call.sessions.get<UserSession>()?.role?.name
                val userRole = roleStr?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

                val result = documentService.getDocumentResponse(userId, documentId, userRole)

                result.fold(
                    onSuccess = { document ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.success(document, "Document retrieved successfully")
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is NotFoundException -> {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            is AuthorizationException -> {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            else -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ApiError(
                                        code = ErrorCodes.INTERNAL_ERROR,
                                        message = error.message ?: "Failed to retrieve document"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            // ========================
            // GET /api/v1/documents/case/{caseNumber}
            // List all documents and evidence records associated with a specific case
            // ========================
            get("/case/{caseNumber}") {
                val caseNumber = call.parameters["caseNumber"]
                if (caseNumber.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(code = ErrorCodes.BAD_REQUEST, message = "Case number is required"))
                    return@get
                }

                val summaries = documentService.getDocumentsByCaseNumber(caseNumber)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(
                        DocumentListResponse(documents = summaries, total = summaries.size),
                        "Documents for case $caseNumber retrieved successfully"
                    )
                )
            }

            // ========================
            // GET /api/v1/documents/department/{dept}
            // List documents assigned to a specific department (e.g. FORENSICS_TEAM)
            // ========================
            get("/department/{dept}") {
                val dept = call.parameters["dept"]
                if (dept.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(code = ErrorCodes.BAD_REQUEST, message = "Department name is required"))
                    return@get
                }

                val summaries = documentService.getDocumentsByDepartment(dept)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(
                        DocumentListResponse(documents = summaries, total = summaries.size),
                        "Documents for department $dept retrieved successfully"
                    )
                )
            }

            // ========================
            // GET /api/v1/documents/{id}/download
            // Secure download of encrypted payload + cryptographic verification hash
            // ========================
            get("/{id}/download") {
                val userId = extractDocumentUserId(call) ?: return@get
                val documentId = call.parameters["id"]

                if (documentId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(code = ErrorCodes.BAD_REQUEST, message = "Document ID is required"))
                    return@get
                }

                val jwtPrincipal = call.principal<JWTPrincipal>()
                val roleStr = jwtPrincipal?.payload?.getClaim("role")?.asString()
                    ?: call.sessions.get<UserSession>()?.role?.name
                val userRole = roleStr?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

                val result = documentService.getDocumentDownload(userId, documentId, userRole)

                result.fold(
                    onSuccess = { downloadResponse ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.success(downloadResponse, "Document payload retrieved successfully")
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is NotFoundException -> call.respond(HttpStatusCode.NotFound, ApiError(code = error.code, message = error.message))
                            is AuthorizationException -> call.respond(HttpStatusCode.Forbidden, ApiError(code = error.code, message = error.message))
                            else -> call.respond(HttpStatusCode.InternalServerError, ApiError(code = ErrorCodes.INTERNAL_ERROR, message = error.message ?: "Failed to download document"))
                        }
                    }
                )
            }

            // ========================
            // GET /api/v1/documents/type/{type}
            // List documents filtered by type for the current user
            // ========================
            get("/type/{type}") {
                val userId = extractDocumentUserId(call) ?: return@get
                val typeParam = call.parameters["type"]

                if (typeParam.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "Document type is required"
                        )
                    )
                    return@get
                }

                val documentType = try {
                    DocumentType.valueOf(typeParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.DOCUMENT_TYPE_NOT_SUPPORTED,
                            message = "Invalid document type: $typeParam. Valid types: ${DocumentType.entries.joinToString(", ") { it.name }}"
                        )
                    )
                    return@get
                }

                val documents = documentService.getDocumentByUserAndType(userId, documentType)
                val summaries = documents.map { it.toSummary() }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(
                        DocumentListResponse(documents = summaries, total = summaries.size),
                        "Documents of type ${documentType.name} retrieved successfully"
                    )
                )
            }

            // ========================
            // PATCH /api/v1/documents/{id}/status
            // Update document status (user can mark their own documents)
            // ========================
            patch("/{id}/status") {
                val userId = extractDocumentUserId(call) ?: return@patch
                val documentId = call.parameters["id"]

                if (documentId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "Document ID is required"
                        )
                    )
                    return@patch
                }

                val request = call.receive<UpdateDocumentStatusRequest>()

                val jwtPrincipal = call.principal<JWTPrincipal>()
                val roleStr = jwtPrincipal?.payload?.getClaim("role")?.asString()
                    ?: call.sessions.get<UserSession>()?.role?.name
                val userRole = roleStr?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

                val result = documentService.updateDocumentStatus(
                    documentId = documentId,
                    status = request.status,
                    userId = userId,
                    userRole = userRole
                )

                result.fold(
                    onSuccess = { document ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.success(document, "Document status updated to ${request.status.name}")
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is NotFoundException -> {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            is AuthorizationException -> {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            else -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ApiError(
                                        code = ErrorCodes.INTERNAL_ERROR,
                                        message = error.message ?: "Failed to update document status"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            // ========================
            // DELETE /api/v1/documents/{id}
            // Delete a document from the user's vault
            // ========================
            delete("/{id}") {
                val userId = extractDocumentUserId(call) ?: return@delete
                val documentId = call.parameters["id"]

                if (documentId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "Document ID is required"
                        )
                    )
                    return@delete
                }

                val result = documentService.deleteDocument(userId, documentId)

                result.fold(
                    onSuccess = {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.ok("Document deleted successfully from your vault")
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is NotFoundException -> {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            is AuthorizationException -> {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            else -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ApiError(
                                        code = ErrorCodes.INTERNAL_ERROR,
                                        message = error.message ?: "Failed to delete document"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            // ========================
            // GET /api/v1/documents/{id}/verify-integrity
            // Verify the integrity of a stored document (hash check)
            // ========================
            get("/{id}/verify-integrity") {
                val userId = extractDocumentUserId(call) ?: return@get
                val documentId = call.parameters["id"]

                if (documentId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "Document ID is required"
                        )
                    )
                    return@get
                }

                // First verify ownership
                val docResult = documentService.getDocumentResponse(userId, documentId)
                if (docResult.isFailure) {
                    val error = docResult.exceptionOrNull()
                    when (error) {
                        is NotFoundException -> {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError(
                                    code = (error as? LegitException)?.code ?: ErrorCodes.DOCUMENT_NOT_FOUND,
                                    message = error.message ?: "Document not found"
                                )
                            )
                        }
                        is AuthorizationException -> {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                ApiError(
                                    code = (error as? LegitException)?.code ?: ErrorCodes.FORBIDDEN,
                                    message = error.message ?: "Access denied"
                                )
                            )
                        }
                        else -> {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError(
                                    code = ErrorCodes.INTERNAL_ERROR,
                                    message = error?.message ?: "Failed to verify document"
                                )
                            )
                        }
                    }
                    return@get
                }

                val integrityResult = documentService.verifyDocumentIntegrity(documentId)

                integrityResult.fold(
                    onSuccess = { isIntact ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.success(
                                IntegrityCheckResponse(
                                    documentId = documentId,
                                    integrityIntact = isIntact,
                                    message = if (isIntact) {
                                        "Document integrity verified — data has not been tampered with"
                                    } else {
                                        "WARNING: Document integrity check failed — data may have been tampered with"
                                    },
                                    checkedAt = System.currentTimeMillis()
                                ),
                                if (isIntact) "Integrity check passed" else "Integrity check failed"
                            )
                        )
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError(
                                code = ErrorCodes.INTERNAL_ERROR,
                                message = error.message ?: "Failed to verify document integrity"
                            )
                        )
                    }
                )
            }

            // ========================
            // GET /api/v1/documents/types
            // List all supported document types
            // ========================
            get("/types/supported") {
                val types = DocumentType.entries.map { type ->
                    SupportedDocumentType(
                        type = type,
                        name = type.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        description = getDocumentTypeDescription(type)
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(types, "Supported document types")
                )
            }
        }
    }

    // ========================
    // ADMIN ROUTES for document management
    // ========================
    route("/api/v1/admin/documents") {
        authenticate("auth-admin") {

            // ========================
            // PATCH /api/v1/admin/documents/{id}/status
            // Admin can update any document's status (e.g., mark as verified)
            // ========================
            patch("/{id}/status") {
                val documentId = call.parameters["id"]

                if (documentId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "Document ID is required"
                        )
                    )
                    return@patch
                }

                val request = call.receive<UpdateDocumentStatusRequest>()

                // Admin can update without ownership check (userId = null)
                val result = documentService.updateDocumentStatus(
                    documentId = documentId,
                    status = request.status,
                    userId = null
                )

                result.fold(
                    onSuccess = { document ->
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.success(document, "Document status updated to ${request.status.name} by admin")
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is NotFoundException -> {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError(
                                        code = error.code,
                                        message = error.message
                                    )
                                )
                            }
                            else -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ApiError(
                                        code = ErrorCodes.INTERNAL_ERROR,
                                        message = error.message ?: "Failed to update document status"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            // ========================
            // GET /api/v1/admin/documents/user/{userId}
            // Admin can view any user's documents
            // ========================
            get("/user/{userId}") {
                val userId = call.parameters["userId"]

                if (userId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError(
                            code = ErrorCodes.BAD_REQUEST,
                            message = "User ID is required"
                        )
                    )
                    return@get
                }

                val documentList = documentService.getDocumentsByUserId(userId)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(documentList, "Documents for user $userId retrieved successfully")
                )
            }
        }
    }
}

// ========================
// DTOs specific to document routes
// ========================

@kotlinx.serialization.Serializable
data class UpdateDocumentStatusRequest(
    val status: DocumentStatus
)

@kotlinx.serialization.Serializable
data class IntegrityCheckResponse(
    val documentId: String,
    val integrityIntact: Boolean,
    val message: String,
    val checkedAt: Long
)

@kotlinx.serialization.Serializable
data class SupportedDocumentType(
    val type: DocumentType,
    val name: String,
    val description: String
)

// ========================
// HELPERS
// ========================

private fun getDocumentTypeDescription(type: DocumentType): String {
    return when (type) {
        DocumentType.FIR -> "First Information Report registered under relevant penal sections"
        DocumentType.POLICE_REPORT -> "Official police case report and station diary entry"
        DocumentType.INVESTIGATION_RECORD -> "Detailed investigation diary and findings dossier"
        DocumentType.WITNESS_STATEMENT -> "Recorded statement of witness under legal provisions"
        DocumentType.CHARGE_SHEET -> "Final investigative charge sheet submitted before court"
        DocumentType.COURT_FILING -> "Legal affidavit, petition, or official court filing"
        DocumentType.EVIDENCE_RECORD -> "Chain-of-custody recorded physical or digital evidence"
        DocumentType.FORENSIC_REPORT -> "Forensic laboratory analysis, ballistic, or cyber report"
        DocumentType.LEGAL_NOTICE -> "Official statutory notice or legal summons"
        DocumentType.AADHAAR_CARD -> "12-digit unique identity number issued by UIDAI"
        DocumentType.PAN_CARD -> "Permanent Account Number issued by the Income Tax Department"
        DocumentType.PASSPORT -> "Travel document issued by the Government of India"
        DocumentType.DRIVING_LICENSE -> "License to drive motor vehicles issued by RTO"
        DocumentType.VOTER_ID -> "Electoral photo identity card issued by the Election Commission"
        DocumentType.BANK_STATEMENT -> "Official bank account statement"
        DocumentType.ADDRESS_PROOF -> "Any valid address proof document"
        DocumentType.INCOME_PROOF -> "Salary slips, ITR, or other income proof documents"
        DocumentType.EDUCATION_CERTIFICATE -> "Degree, diploma, or other educational certificates"
        DocumentType.OTHER -> "Any other document type not listed above"
    }
}

/**
 * Extracts userId from either JWT principal or session.
 * Returns null and sends an error response if neither is available.
 */
private suspend fun extractDocumentUserId(call: ApplicationCall): String? {
    // Try JWT first
    val jwtPrincipal = call.principal<JWTPrincipal>()
    if (jwtPrincipal != null) {
        return jwtPrincipal.payload.subject
    }

    // Fall back to session
    val session = call.sessions.get<UserSession>()
    if (session != null) {
        return session.userId
    }

    call.respond(
        HttpStatusCode.Unauthorized,
        ApiError(
            code = ErrorCodes.UNAUTHORIZED,
            message = "Could not identify user. Please provide a valid token or session."
        )
    )
    return null
}
