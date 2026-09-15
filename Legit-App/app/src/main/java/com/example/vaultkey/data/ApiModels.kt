package com.example.vaultkey.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = 0L
)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L
)

@Serializable
data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String? = null
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val legitId: String? = null,
    val role: String,
    val expiresIn: Long
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val legitId: String? = null,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val isVerified: Boolean,
    val role: String,
    val createdAt: Long
)

@Serializable
data class UpdateProfileRequestDto(
    val fullName: String? = null,
    val phoneNumber: String? = null
)

@Serializable
data class DocumentMetadataDto(
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val fatherName: String? = null,
    val gender: String? = null,
    val caseNumber: String? = null,
    val officerName: String? = null,
    val officerBadge: String? = null,
    val targetDepartment: String? = null,
    val targetRecipientId: String? = null,
    val evidenceCategory: String? = null,
    val priority: String? = null,
    val chainOfCustodyNotes: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val clientEncrypted: Boolean = false,
    val clientKeyWrap: String? = null,
    val extraFields: Map<String, String> = emptyMap()
)

@Serializable
data class DocumentSummaryDto(
    val id: String,
    val documentType: String,
    val documentName: String,
    @SerialName("documentNumberMasked")
    val documentNumberMasked: String,
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val dataHash: String = "",
    val status: String,
    val createdAt: Long
)

@Serializable
data class DocumentListResponseDto(
    val documents: List<DocumentSummaryDto>,
    val total: Int
)

@Serializable
data class DocumentResponseDto(
    val id: String,
    val userId: String,
    val documentType: String,
    val documentNumber: String,
    val documentName: String,
    val metadata: DocumentMetadataDto,
    val dataHash: String = "",
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val status: String,
    val issuedBy: String? = null,
    val issuedAt: Long? = null,
    val expiresAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class DocumentUploadRequestDto(
    val documentType: String,
    val documentNumber: String,
    val documentName: String,
    val metadata: DocumentMetadataDto,
    val rawData: String,
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val targetRecipientId: String? = null,
    val issuedBy: String? = null,
    val issuedAt: Long? = null,
    val expiresAt: Long? = null
)

@Serializable
data class ContractSummaryDto(
    val contractId: String,
    val requesterName: String,
    val purpose: String,
    val requiredDocumentTypes: List<String>,
    val requiredFields: List<String>,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long
)

@Serializable
data class VerificationResponseDto(
    val contractId: String,
    val requesterName: String,
    val userId: String?,
    val purpose: String,
    val status: String,
    // Add these fields which might be missing in VerificationResponse but needed by the UI
    // If backend doesn't provide them in VerificationResponse, we might need to adjust backend 
    // or use ContractSummary if possible.
    val requiredDocumentTypes: List<String> = emptyList(),
    val requiredFields: List<String> = emptyList(),
    val createdAt: Long,
    val verifiedAt: Long? = null,
    val metadata: DocumentMetadataDto? = null
)

@Serializable
data class ContractListResponseDto(
    val contracts: List<ContractSummaryDto>,
    val total: Int
)

@Serializable
data class ContractApprovalRequestDto(
    val contractId: String,
    val approved: Boolean
)

@Serializable
data class DisposableKeyResponseDto(
    val contractId: String,
    val disposableKey: String,
    val expiresAt: Long,
    val message: String
)

@Serializable
data class RegisterPushTokenRequestDto(
    val fcmToken: String,
    val deviceType: String = "ANDROID"
)

data class SessionData(
    val token: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val legitId: String? = null,
    val role: String
)

data class SignupForm(
    val username: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val password: String
)

data class UploadDocumentForm(
    val documentType: String,
    val documentName: String,
    val documentNumber: String,
    val issuedBy: String,
    val fullName: String,
    val dateOfBirth: String,
    val address: String,
    val fatherName: String,
    val gender: String
)

data class UploadEvidenceForm(
    val documentType: String,
    val documentName: String,
    val documentNumber: String,
    val caseNumber: String,
    val officerName: String,
    val officerBadge: String,
    val targetDepartment: String,
    val evidenceCategory: String,
    val priority: String,
    val chainOfCustodyNotes: String,
    val fileBytes: ByteArray?,
    val fileName: String?,
    val fileMimeType: String?,
    val fileSizeBytes: Long?
)
