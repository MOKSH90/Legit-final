package com.example.vaultkey.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.vaultkey.crypto.CryptoEngine
import com.example.vaultkey.data.*
import com.example.vaultkey.utils.FileUtils
import com.example.vaultkey.utils.SelectedFileData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val evidenceDocumentTypes = listOf(
    "FIR",
    "POLICE_REPORT",
    "INVESTIGATION_RECORD",
    "WITNESS_STATEMENT",
    "CHARGE_SHEET",
    "COURT_FILING",
    "EVIDENCE_RECORD",
    "FORENSIC_REPORT",
    "LEGAL_NOTICE"
)

private val civilDocumentTypes = listOf(
    "AADHAAR_CARD",
    "PAN_CARD",
    "PASSPORT",
    "DRIVING_LICENSE",
    "VOTER_ID",
    "BANK_STATEMENT",
    "ADDRESS_PROOF",
    "INCOME_PROOF",
    "EDUCATION_CERTIFICATE",
    "OTHER"
)

private val targetDepartments = listOf(
    "FORENSICS_TEAM",
    "CYBER_FORENSICS_LAB",
    "BALLISTICS_DIVISION",
    "DISTRICT_SESSIONS_COURT",
    "CRIME_INVESTIGATION_DEPT",
    "INTERNAL_AFFAIRS"
)

private val priorityLevels = listOf(
    "CRITICAL",
    "HIGH",
    "MEDIUM",
    "LOW"
)

data class IssuedDoc(
    val id: String,
    val title: String,
    val subtitle: String,
    val issuedDate: String,
    val caseNumber: String? = null,
    val targetDepartment: String? = null,
    val dataHash: String = "",
    val icon: ImageVector,
    val type: String,
    val containerColor: @Composable () -> Color,
    val contentColor: @Composable () -> Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Documents(
    navController: NavHostController,
    appController: AppController
) {
    val focusManager = LocalFocusManager.current
    var search by remember { mutableStateOf("") }
    var activeSheetDoc by remember { mutableStateOf<IssuedDoc?>(null) }
    var showUploadSheet by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        appController.refreshDocuments()
    }

    val documents = appController.documents.map {
        val isEvidence = evidenceDocumentTypes.contains(it.documentType)
        IssuedDoc(
            id = it.id,
            title = it.documentName,
            subtitle = it.documentType.replace('_', ' '),
            issuedDate = it.documentNumberMasked,
            caseNumber = it.caseNumber,
            targetDepartment = it.targetDepartment,
            dataHash = it.dataHash,
            icon = when (it.documentType) {
                "FIR", "POLICE_REPORT" -> Icons.Outlined.Shield
                "FORENSIC_REPORT" -> Icons.Outlined.Biotech
                "EVIDENCE_RECORD" -> Icons.Outlined.FolderZip
                "WITNESS_STATEMENT" -> Icons.Outlined.RecordVoiceOver
                "CHARGE_SHEET", "COURT_FILING", "LEGAL_NOTICE" -> Icons.Outlined.Gavel
                "AADHAAR_CARD" -> Icons.Rounded.Badge
                "PAN_CARD" -> Icons.Rounded.CreditCard
                else -> Icons.Rounded.Description
            },
            type = it.documentType,
            containerColor = {
                if (isEvidence) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = {
                if (isEvidence) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }

    val filteredDocs = documents.filter {
        it.title.contains(search, ignoreCase = true) ||
            it.subtitle.contains(search, ignoreCase = true) ||
            (it.caseNumber?.contains(search, ignoreCase = true) ?: false)
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Case & Vault Records", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showUploadSheet = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "Add") },
                    text = { Text("Upload Evidence / Doc") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            textFieldOut(
                values = search,
                placeholder = "Search Evidence, Case #, or Documents",
                icon = Icons.Outlined.Search,
                onValueChange = { search = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            appController.consumeMessage()?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredDocs) { doc ->
                    IssuedDocItem(doc = doc, onClick = { 
                        appController.fetchDocumentDetails(doc.id)
                        activeSheetDoc = doc 
                    })
                }
            }
        }

        activeSheetDoc?.let { _ ->
            ModalBottomSheet(
                onDismissRequest = { 
                    activeSheetDoc = null
                    appController.clearCurrentDocument()
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                val doc = appController.currentDocument
                if (doc != null) {
                    DocumentViewSheet(
                        docType = doc.documentType,
                        docTitle = doc.documentName,
                        holderName = doc.metadata.officerName ?: doc.metadata.fullName ?: appController.profile?.fullName,
                        docNumberMasked = doc.documentNumber,
                        holderDob = doc.metadata.dateOfBirth,
                        onDismiss = { 
                            activeSheetDoc = null
                            appController.clearCurrentDocument()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
            }
        }

        if (showUploadSheet) {
            ModalBottomSheet(
                onDismissRequest = { showUploadSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                UploadEvidenceAndDocSheet(
                    profile = appController.profile,
                    onDismiss = { showUploadSheet = false },
                    onUploadEvidence = { form ->
                        appController.uploadEvidence(form) {
                            showUploadSheet = false
                        }
                    },
                    onUploadCivil = { form ->
                        appController.uploadDocument(form)
                        showUploadSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadEvidenceAndDocSheet(
    profile: UserProfileDto?,
    onDismiss: () -> Unit,
    onUploadEvidence: (UploadEvidenceForm) -> Unit,
    onUploadCivil: (UploadDocumentForm) -> Unit
) {
    val context = LocalContext.current
    var uploadMode by remember { mutableStateOf(0) } // 0 = Evidence / Police Report, 1 = Civil Identity

    // Evidence fields
    var docType by remember { mutableStateOf("FIR") }
    var caseNumber by remember { mutableStateOf("") }
    var docName by remember { mutableStateOf("") }
    var docRefNumber by remember { mutableStateOf("") }
    var officerName by remember(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }
    var officerBadge by remember(profile?.legitId) { mutableStateOf(profile?.legitId.orEmpty()) }
    var targetDept by remember { mutableStateOf("FORENSICS_TEAM") }
    var priority by remember { mutableStateOf("HIGH") }
    var custodyNotes by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<SelectedFileData?>(null) }
    var fileHash by remember { mutableStateOf<String?>(null) }

    // Dropdown states
    var typeExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    // Civil Doc fields
    var civilDocType by remember { mutableStateOf("AADHAAR_CARD") }
    var civilDocName by remember { mutableStateOf("") }
    var civilDocNumber by remember { mutableStateOf("") }
    var civilIssuedBy by remember { mutableStateOf("") }
    var civilFullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }
    var civilDob by remember { mutableStateOf("") }
    var civilAddress by remember { mutableStateOf("") }
    var civilFatherName by remember { mutableStateOf("") }
    var civilGender by remember { mutableStateOf("") }
    var civilTypeExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileData = FileUtils.readUriData(context, uri)
            selectedFile = fileData
            if (fileData != null) {
                fileHash = CryptoEngine.generateSha256(fileData.bytes)
                if (docName.isBlank()) {
                    docName = fileData.fileName.substringBeforeLast('.')
                }
                if (docRefNumber.isBlank()) {
                    docRefNumber = "EVD-" + System.currentTimeMillis().toString().takeLast(6)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uploadMode == 0) "Secure Evidence Dispatch" else "Add Identity Document",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Mode Selector Tabs
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = uploadMode == 0,
                onClick = { uploadMode = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Outlined.Security, contentDescription = null) }
            ) {
                Text("Police / Forensics", fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = uploadMode == 1,
                onClick = { uploadMode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Outlined.Badge, contentDescription = null) }
            ) {
                Text("Civil Vault")
            }
        }

        if (uploadMode == 0) {
            // ========================
            // POLICE / FORENSICS MODE
            // ========================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "End-to-End Cryptographic Sealing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Documents are encrypted locally with AES-256-GCM. SHA-256 hash guarantees zero tampering across the chain of custody.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            UploadSectionCard(
                title = "File Attachment & Hashing",
                subtitle = "Select evidence PDF, forensic report, or media file to compute cryptographic hash."
            ) {
                if (selectedFile != null) {
                    ElevatedCard(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedFile!!.fileName, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(selectedFile!!.formattedSize + " • " + selectedFile!!.mimeType, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                    Icon(Icons.Outlined.ChangeCircle, contentDescription = "Change")
                                }
                            }
                            if (fileHash != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("SHA-256 Digest:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    fileHash!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Select Document / PDF File", fontWeight = FontWeight.Bold)
                    }
                }
            }

            UploadSectionCard(
                title = "Case & Evidence Details",
                subtitle = "Link this report to an ongoing investigation and assigned department."
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = docType.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type") },
                        leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        evidenceDocumentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.replace('_', ' ')) },
                                onClick = {
                                    docType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = "Case / Crime Number (e.g. CR-2026/089)",
                    icon = Icons.Outlined.Numbers
                )

                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = "Evidence / Report Title",
                    icon = Icons.Outlined.Description
                )

                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = docRefNumber,
                    onValueChange = { docRefNumber = it },
                    label = "Reference / Tag Number",
                    icon = Icons.Outlined.Tag
                )

                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = deptExpanded,
                    onExpandedChange = { deptExpanded = !deptExpanded }
                ) {
                    OutlinedTextField(
                        value = targetDept.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To Department / Authority") },
                        leadingIcon = { Icon(Icons.Outlined.AccountBalance, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = deptExpanded,
                        onDismissRequest = { deptExpanded = false }
                    ) {
                        targetDepartments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept.replace('_', ' ')) },
                                onClick = {
                                    targetDept = dept
                                    deptExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority Level") },
                        leadingIcon = { Icon(Icons.Outlined.PriorityHigh, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        priorityLevels.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    priority = p
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            UploadSectionCard(
                title = "Officer & Chain of Custody",
                subtitle = "Uploader officer authentication and initial transfer log."
            ) {
                UploadField(
                    value = officerName,
                    onValueChange = { officerName = it },
                    label = "Submitting Officer Name",
                    icon = Icons.Outlined.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = officerBadge,
                    onValueChange = { officerBadge = it },
                    label = "Badge / Dedox ID",
                    icon = Icons.Outlined.Badge
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = custodyNotes,
                    onValueChange = { custodyNotes = it },
                    label = "Chain of Custody Notes / Instructions for Forensics",
                    icon = Icons.Outlined.Notes,
                    singleLine = false,
                    minLines = 3
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onUploadEvidence(
                            UploadEvidenceForm(
                                documentType = docType,
                                documentName = docName.ifBlank { "Evidence Report" },
                                documentNumber = docRefNumber.ifBlank { "REF-" + System.currentTimeMillis() },
                                caseNumber = caseNumber,
                                officerName = officerName,
                                officerBadge = officerBadge,
                                targetDepartment = targetDept,
                                evidenceCategory = docType,
                                priority = priority,
                                chainOfCustodyNotes = custodyNotes,
                                fileBytes = selectedFile?.bytes,
                                fileName = selectedFile?.fileName,
                                fileMimeType = selectedFile?.mimeType,
                                fileSizeBytes = selectedFile?.sizeBytes
                            )
                        )
                    },
                    enabled = docName.isNotBlank() || selectedFile != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Seal & Dispatch")
                }
            }
        } else {
            // ========================
            // CIVIL IDENTITY VAULT MODE
            // ========================
            UploadSectionCard(
                title = "Civil Document Details",
                subtitle = "Choose document type and enter identification data."
            ) {
                ExposedDropdownMenuBox(
                    expanded = civilTypeExpanded,
                    onExpandedChange = { civilTypeExpanded = !civilTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = civilDocType.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type") },
                        leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = civilTypeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = civilTypeExpanded,
                        onDismissRequest = { civilTypeExpanded = false }
                    ) {
                        civilDocumentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.replace('_', ' ')) },
                                onClick = {
                                    civilDocType = type
                                    civilTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilDocName,
                    onValueChange = { civilDocName = it },
                    label = "Document Name",
                    icon = Icons.Outlined.Description
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilDocNumber,
                    onValueChange = { civilDocNumber = it },
                    label = "Document Number",
                    icon = Icons.Outlined.CreditCard
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilIssuedBy,
                    onValueChange = { civilIssuedBy = it },
                    label = "Issued By Authority",
                    icon = Icons.AutoMirrored.Outlined.Article
                )
            }

            UploadSectionCard(
                title = "Holder Identity Details",
                subtitle = "Personal verification information."
            ) {
                UploadField(
                    value = civilFullName,
                    onValueChange = { civilFullName = it },
                    label = "Full Name",
                    icon = Icons.Outlined.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilDob,
                    onValueChange = { civilDob = it },
                    label = "Date of Birth (yyyy-mm-dd)",
                    icon = Icons.Outlined.CalendarMonth
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilAddress,
                    onValueChange = { civilAddress = it },
                    label = "Address",
                    icon = Icons.Outlined.PinDrop,
                    singleLine = false,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilFatherName,
                    onValueChange = { civilFatherName = it },
                    label = "Father Name",
                    icon = Icons.Outlined.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                UploadField(
                    value = civilGender,
                    onValueChange = { civilGender = it },
                    label = "Gender",
                    icon = Icons.Outlined.Transgender
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onUploadCivil(
                            UploadDocumentForm(
                                documentType = civilDocType,
                                documentName = civilDocName,
                                documentNumber = civilDocNumber,
                                issuedBy = civilIssuedBy,
                                fullName = civilFullName,
                                dateOfBirth = civilDob,
                                address = civilAddress,
                                fatherName = civilFatherName,
                                gender = civilGender
                            )
                        )
                    },
                    enabled = civilDocNumber.isNotBlank() && civilDocName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Save to Vault")
                }
            }
        }
    }
}

@Composable
private fun UploadSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun UploadField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    supporting: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = supporting?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        )
    )
}

@Composable
fun IssuedDocItem(
    doc: IssuedDoc,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(16.dp),
                color = doc.containerColor(),
                contentColor = doc.contentColor()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = doc.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = doc.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!doc.caseNumber.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Case: ${doc.caseNumber}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (!doc.targetDepartment.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = doc.targetDepartment.replace('_', ' '),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
