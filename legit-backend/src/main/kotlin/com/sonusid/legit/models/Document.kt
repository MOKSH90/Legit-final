package com.sonusid.legit.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
enum class DocumentType {
    // Law Enforcement & Evidence Types
    FIR,
    POLICE_REPORT,
    INVESTIGATION_RECORD,
    WITNESS_STATEMENT,
    CHARGE_SHEET,
    COURT_FILING,
    EVIDENCE_RECORD,
    FORENSIC_REPORT,
    LEGAL_NOTICE,
    // Civil & Identity Types
    AADHAAR_CARD,
    PAN_CARD,
    PASSPORT,
    DRIVING_LICENSE,
    VOTER_ID,
    BANK_STATEMENT,
    ADDRESS_PROOF,
    INCOME_PROOF,
    EDUCATION_CERTIFICATE,
    OTHER
}

@Serializable
enum class DocumentStatus {
    PENDING,
    SUBMITTED,
    ASSIGNED_TO_FORENSICS,
    UNDER_ANALYSIS,
    FORENSIC_VERIFIED,
    VERIFIED,
    REJECTED,
    EXPIRED
}

@Serializable
data class Document(
    @SerialName("_id")
    @Contextual
    val id: ObjectId? = null,
    val userId: String,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentName: String,
    val metadata: DocumentMetadata,
    val dataHash: String,
    val encryptedData: String,
    val status: DocumentStatus = DocumentStatus.PENDING,
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val targetRecipientId: String? = null,
    val issuedBy: String? = null,
    val issuedAt: Long? = null,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class DocumentMetadata(
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val fatherName: String? = null,
    val gender: String? = null,
    // Law Enforcement & Evidence Fields
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
data class DocumentUploadRequest(
    val documentType: DocumentType,
    val documentNumber: String,
    val documentName: String,
    val metadata: DocumentMetadata,
    val rawData: String,
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val targetRecipientId: String? = null,
    val issuedBy: String? = null,
    val issuedAt: Long? = null,
    val expiresAt: Long? = null
)

@Serializable
data class DocumentResponse(
    val id: String,
    val userId: String,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentName: String,
    val metadata: DocumentMetadata,
    val dataHash: String = "",
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val status: DocumentStatus,
    val issuedBy: String?,
    val issuedAt: Long?,
    val expiresAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class DocumentListResponse(
    val documents: List<DocumentSummary>,
    val total: Int
)

@Serializable
data class DocumentSummary(
    val id: String,
    val documentType: DocumentType,
    val documentName: String,
    val documentNumberMasked: String,
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val dataHash: String = "",
    val status: DocumentStatus,
    val createdAt: Long
)

@Serializable
data class DocumentDownloadResponse(
    val id: String,
    val documentType: DocumentType,
    val documentName: String,
    val documentNumber: String,
    val caseNumber: String?,
    val dataHash: String,
    val encryptedData: String,
    val clientEncrypted: Boolean,
    val clientKeyWrap: String?,
    val metadata: DocumentMetadata,
    val verifiedIntegrity: Boolean
)

@Serializable
data class ChainOfCustodyEvent(
    val eventId: String,
    val documentId: String,
    val caseNumber: String?,
    val action: String,
    val performedBy: String,
    val performedByRole: String,
    val recipientDepartment: String?,
    val dataHash: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

fun Document.toResponse(): DocumentResponse = DocumentResponse(
    id = id?.toHexString() ?: "",
    userId = userId,
    documentType = documentType,
    documentNumber = maskDocumentNumber(documentNumber, documentType),
    documentName = documentName,
    metadata = metadata,
    dataHash = dataHash,
    caseNumber = caseNumber ?: metadata.caseNumber,
    targetDepartment = targetDepartment ?: metadata.targetDepartment,
    status = status,
    issuedBy = issuedBy,
    issuedAt = issuedAt,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Document.toSummary(): DocumentSummary = DocumentSummary(
    id = id?.toHexString() ?: "",
    documentType = documentType,
    documentName = documentName,
    documentNumberMasked = maskDocumentNumber(documentNumber, documentType),
    caseNumber = caseNumber ?: metadata.caseNumber,
    targetDepartment = targetDepartment ?: metadata.targetDepartment,
    dataHash = dataHash,
    status = status,
    createdAt = createdAt
)

fun maskDocumentNumber(number: String, type: DocumentType): String {
    if (number.length <= 4) return "****"
    return when (type) {
        DocumentType.AADHAAR_CARD -> "XXXX-XXXX-" + number.takeLast(4)
        DocumentType.PAN_CARD -> number.take(2) + "XXXXX" + number.takeLast(3)
        DocumentType.FIR,
        DocumentType.POLICE_REPORT,
        DocumentType.INVESTIGATION_RECORD,
        DocumentType.WITNESS_STATEMENT,
        DocumentType.CHARGE_SHEET,
        DocumentType.COURT_FILING,
        DocumentType.EVIDENCE_RECORD,
        DocumentType.FORENSIC_REPORT,
        DocumentType.LEGAL_NOTICE -> number // Don't mask case/FIR reference numbers
        else -> "*".repeat(number.length - 4) + number.takeLast(4)
    }
}
