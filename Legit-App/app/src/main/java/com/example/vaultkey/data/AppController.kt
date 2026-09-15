package com.example.vaultkey.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.vaultkey.crypto.CryptoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppController(
    private val api: LegitApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var session by mutableStateOf(api.currentSession())
        private set
    var profile by mutableStateOf<UserProfileDto?>(null)
        private set
    var documents by mutableStateOf<List<DocumentSummaryDto>>(emptyList())
        private set
    var currentDocument by mutableStateOf<DocumentResponseDto?>(null)
        private set
    var scannedContract by mutableStateOf<VerificationResponseDto?>(null)
        private set
    var pendingContracts by mutableStateOf<List<ContractSummaryDto>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    val validationErrors = mutableStateListOf<String>()

    init {
        if (session != null) {
            refreshAll()
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        runRequest(onSuccess = onSuccess) {
            val auth = api.login(LoginRequestDto(email = email.trim(), password = password))
            api.persistSession(auth)
            session = api.currentSession()
            refreshAll()
        }
    }

    fun signup(form: SignupForm, onSuccess: () -> Unit) {
        runRequest(onSuccess = onSuccess) {
            val auth = api.register(
                RegisterRequestDto(
                    username = form.username.trim(),
                    email = form.email.trim(),
                    password = form.password,
                    fullName = form.fullName.trim(),
                    phoneNumber = form.phoneNumber.trim().ifBlank { null }
                )
            )
            api.persistSession(auth)
            session = api.currentSession()
            refreshAll()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        api.clearSession()
        session = null
        profile = null
        documents = emptyList()
        pendingContracts = emptyList()
        message = null
        validationErrors.clear()
        onLoggedOut()
    }

    fun refreshAll() {
        if (session == null) return
        refreshProfile()
        refreshDocuments()
        refreshPendingContracts()
    }

    fun refreshProfile() {
        if (session == null) return
        runRequest {
            profile = api.getProfile()
        }
    }

    fun refreshDocuments() {
        if (session == null) return
        runRequest {
            documents = api.getDocuments().documents
        }
    }

    fun fetchDocumentDetails(documentId: String) {
        if (session == null) return
        runRequest {
            currentDocument = api.getDocument(documentId)
        }
    }

    fun clearCurrentDocument() {
        currentDocument = null
    }

    fun fetchScannedContract(contractId: String, onSuccess: () -> Unit, onFailure: (() -> Unit)? = null) {
        if (session == null) {
            onFailure?.invoke()
            return
        }
        runRequest(onSuccess = onSuccess, onFailure = onFailure) {
            scannedContract = api.getContract(contractId)
        }
    }

    fun clearScannedContract() {
        scannedContract = null
    }

    fun refreshPendingContracts() {
        if (session == null) return
        runRequest {
            pendingContracts = api.getPendingContracts().contracts
        }
    }

    fun registerPushToken(token: String) {
        if (session == null) return
        runRequest {
            api.registerFcmToken(token)
        }
    }

    fun updateProfile(fullName: String, phoneNumber: String) {
        runRequest {
            profile = api.updateProfile(
                UpdateProfileRequestDto(
                    fullName = fullName.trim().ifBlank { null },
                    phoneNumber = phoneNumber.trim().ifBlank { null }
                )
            )
            message = "Profile updated"
        }
    }

    fun uploadDocument(form: UploadDocumentForm) {
        runRequest {
            val fullName = form.fullName.trim().ifBlank { profile?.fullName }
            val phoneNumber = profile?.phoneNumber?.trim().orEmpty().ifBlank { null }
            val email = profile?.email?.trim().orEmpty().ifBlank { null }

            api.uploadDocument(
                DocumentUploadRequestDto(
                    documentType = form.documentType,
                    documentNumber = form.documentNumber.trim(),
                    documentName = form.documentName.trim(),
                    metadata = DocumentMetadataDto(
                        fullName = fullName,
                        dateOfBirth = form.dateOfBirth.trim().ifBlank { null },
                        address = form.address.trim().ifBlank { null },
                        fatherName = form.fatherName.trim().ifBlank { null },
                        gender = form.gender.trim().ifBlank { null },
                        extraFields = buildMap {
                            email?.let { put("email", it) }
                            phoneNumber?.let { put("phoneNumber", it) }
                        }
                    ),
                    rawData = buildRawPayload(
                        form = form,
                        fullName = fullName,
                        email = email,
                        phoneNumber = phoneNumber
                    ),
                    issuedBy = form.issuedBy.trim().ifBlank { null }
                )
            )
            refreshDocuments()
            message = "Document uploaded"
        }
    }

    fun uploadEvidence(form: UploadEvidenceForm, onSuccess: (() -> Unit)? = null) {
        runRequest(onSuccess = onSuccess) {
            val officerName = form.officerName.trim().ifBlank { profile?.fullName ?: session?.username ?: "Police Officer" }
            val officerBadge = form.officerBadge.trim().ifBlank { profile?.legitId ?: "POLICE-REF" }

            // Raw data to encrypt: file bytes if provided, else structured JSON payload
            val rawBytes = form.fileBytes ?: buildJsonObject {
                put("caseNumber", form.caseNumber.trim())
                put("documentType", form.documentType)
                put("documentNumber", form.documentNumber.trim())
                put("documentName", form.documentName.trim())
                put("officerName", officerName)
                put("officerBadge", officerBadge)
                put("targetDepartment", form.targetDepartment.trim())
                put("evidenceCategory", form.evidenceCategory.trim())
                put("priority", form.priority.trim())
                put("chainOfCustodyNotes", form.chainOfCustodyNotes.trim())
                put("timestamp", System.currentTimeMillis())
            }.toString().toByteArray(Charsets.UTF_8)

            // Local Client-Side AES-256-GCM Zero-Knowledge Encryption
            val encryptedResult = CryptoEngine.encryptDataAesGcm(rawBytes)

            val metadata = DocumentMetadataDto(
                fullName = officerName,
                caseNumber = form.caseNumber.trim(),
                officerName = officerName,
                officerBadge = officerBadge,
                targetDepartment = form.targetDepartment.trim(),
                evidenceCategory = form.evidenceCategory.trim(),
                priority = form.priority.trim(),
                chainOfCustodyNotes = form.chainOfCustodyNotes.trim(),
                fileName = form.fileName,
                fileMimeType = form.fileMimeType,
                fileSizeBytes = form.fileSizeBytes ?: rawBytes.size.toLong(),
                clientEncrypted = true,
                clientKeyWrap = encryptedResult.aesKeyBase64
            )

            api.uploadDocument(
                DocumentUploadRequestDto(
                    documentType = form.documentType,
                    documentNumber = form.documentNumber.trim(),
                    documentName = form.documentName.trim(),
                    metadata = metadata,
                    rawData = encryptedResult.ciphertextBase64,
                    caseNumber = form.caseNumber.trim(),
                    targetDepartment = form.targetDepartment.trim(),
                    issuedBy = "$officerName ($officerBadge)"
                )
            )

            refreshDocuments()
            message = "Evidence sealed & uploaded (SHA-256: ${encryptedResult.sha256Hash.take(12)}...)"
        }
    }

    suspend fun getContract(contractId: String): VerificationResponseDto? {
        return try {
            loading = true
            val result = api.getContract(contractId)
            result
        } catch (e: Exception) {
            message = api.mapThrowable(e).message
            null
        } finally {
            loading = false
        }
    }

    fun approveContract(contractId: String, approved: Boolean, onSuccess: (() -> Unit)? = null) {
        runRequest(onSuccess = if (approved) onSuccess else null) {
            api.approveContract(contractId, approved)
            pendingContracts = pendingContracts.filterNot { it.contractId == contractId }
            refreshPendingContracts()
        }
    }

    fun consumeMessage(): String? {
        val current = message
        message = null
        return current
    }

    fun showMessage(msg: String) {
        message = msg
    }

    private fun runRequest(onSuccess: (() -> Unit)? = null, onFailure: (() -> Unit)? = null, action: suspend () -> Unit) {
        scope.launch {
            loading = true
            validationErrors.clear()
            try {
                action()
                onSuccess?.invoke()
            } catch (error: Throwable) {
                val mapped = api.mapThrowable(error)
                if (mapped.statusCode == 401) {
                    // Stale/invalid token in preferences: clear session and stop protected retries.
                    api.clearSession()
                    session = null
                    profile = null
                    documents = emptyList()
                    pendingContracts = emptyList()
                }
                message = mapped.message
                validationErrors.addAll(mapped.details.values)
                onFailure?.invoke()
            } finally {
                loading = false
            }
        }
    }

    private fun buildRawPayload(
        form: UploadDocumentForm,
        fullName: String?,
        email: String?,
        phoneNumber: String?
    ): String {
        return buildJsonObject {
            put("documentType", form.documentType)
            put("documentName", form.documentName.trim())
            put("documentNumber", form.documentNumber.trim())
            put("issuedBy", form.issuedBy.trim())
            put("fullName", fullName.orEmpty())
            put("dateOfBirth", form.dateOfBirth.trim())
            put("address", form.address.trim())
            put("fatherName", form.fatherName.trim())
            put("gender", form.gender.trim())
            put("email", email.orEmpty())
            put("phoneNumber", phoneNumber.orEmpty())
            put("generatedBy", "vaultkey-android")
            put("generatedAt", System.currentTimeMillis())
        }.toString()
    }
}
