package com.example.vaultkey.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.vaultkey.data.AppController
import com.example.vaultkey.data.UploadDocumentForm
import com.example.vaultkey.data.UserProfileDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val supportedDocumentTypes = listOf(
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

data class IssuedDoc(
    val id: String,
    val title: String,
    val subtitle: String,
    val issuedDate: String,
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
        IssuedDoc(
            id = it.id,
            title = it.documentName,
            subtitle = it.documentType.replace('_', ' '),
            issuedDate = it.documentNumberMasked,
            icon = when (it.documentType) {
                "AADHAAR_CARD" -> Icons.Rounded.Badge
                "PAN_CARD" -> Icons.Rounded.CreditCard
                else -> Icons.Rounded.School
            },
            type = it.documentType,
            containerColor = { MaterialTheme.colorScheme.primaryContainer },
            contentColor = { MaterialTheme.colorScheme.onPrimaryContainer }
        )
    }

    val filteredDocs = documents.filter {
        it.title.contains(search, ignoreCase = true) || it.subtitle.contains(search, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Documents", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                    text = { Text("Add Documents") }
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
                placeholder = "Search Documents",
                icon = Icons.Outlined.Search,
                onValueChange = { search = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            appController.consumeMessage()?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
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
                        holderName = doc.metadata.fullName ?: appController.profile?.fullName,
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
                UploadDocumentSheet(
                    profile = appController.profile,
                    onDismiss = { showUploadSheet = false },
                    onUpload = {
                        appController.uploadDocument(it)
                        showUploadSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadDocumentSheet(
    profile: UserProfileDto?,
    onDismiss: () -> Unit,
    onUpload: (UploadDocumentForm) -> Unit
) {
    var documentType by remember { mutableStateOf("AADHAAR_CARD") }
    var documentName by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var issuedBy by remember { mutableStateOf("") }
    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }
    var dateOfBirth by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    var isExtracting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExtracting = true
                // Simulate OCR extraction delay
                delay(2000)
                
                // Simulated extraction logic based on document type
                when (documentType) {
                    "AADHAAR_CARD" -> {
                        documentNumber = (1000..9999).random().toString() + 
                                         (1000..9999).random().toString() + 
                                         (1000..9999).random().toString()
                        documentName = "Aadhaar Card"
                        issuedBy = "UIDAI"
                        dateOfBirth = "2000-01-01"
                        address = "123, Street Name, City, State, 123456"
                        gender = "Male"
                        fatherName = "Random Father Name"
                    }
                    "PAN_CARD" -> {
                        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        val prefix = (1..5).map { letters.random() }.joinToString("")
                        val digits = (1000..9999).random().toString()
                        val suffix = letters.random()
                        documentNumber = prefix + digits + suffix
                        documentName = "PAN Card"
                        issuedBy = "Income Tax Department"
                        dateOfBirth = "2000-01-01"
                        gender = "Male"
                        fatherName = "Random Father Name"
                    }
                }
                isExtracting = false
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
                text = "Add a document",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { launcher.launch("image/*") },
                enabled = !isExtracting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (isExtracting) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                Text(
                    "Extracting metadata from document...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Store a document in your Legit vault with structured identity metadata.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!profile?.fullName.isNullOrBlank()) {
            Text(
                text = "Full name is prefilled from your profile. You can adjust it if this document differs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        UploadSectionCard(
            title = "Document details",
            subtitle = "Choose the type and enter the issued document information."
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = documentType.replace('_', ' '),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Document type") },
                    leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(18.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    supportedDocumentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.replace('_', ' ')) },
                            onClick = {
                                documentType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = documentName,
                onValueChange = { documentName = it },
                label = "Document name",
                icon = Icons.Outlined.Description
            )
            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = documentNumber,
                onValueChange = { input ->
                    val filtered = when (documentType) {
                        "AADHAAR_CARD" -> input.filter { it.isDigit() }.take(12)
                        "PAN_CARD" -> input.uppercase().take(10) // Standard 10 characters
                        else -> input
                    }
                    documentNumber = filtered
                },
                label = "Document number",
                supporting = when (documentType) {
                    "AADHAAR_CARD" -> "Must be 12 digits"
                    "PAN_CARD" -> "Must be 10 characters (e.g. ABCDE1234F)"
                    else -> null
                },
                icon = Icons.Outlined.CreditCard,
                keyboardType = when (documentType) {
                    "AADHAAR_CARD" -> KeyboardType.Number
                    else -> KeyboardType.Text
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = issuedBy,
                onValueChange = { issuedBy = it },
                label = "Issued by",
                icon = Icons.AutoMirrored.Outlined.Article
            )
        }

        UploadSectionCard(
            title = "Holder details",
            subtitle = "These fields are used by the backend verification pipeline."
        ) {
            UploadField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full name",
                icon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words,
                supporting = if (!profile?.fullName.isNullOrBlank()) "Prefilled from your profile" else null
            )
            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = "Date of birth",
                supporting = "Use yyyy-mm-dd",
                icon = Icons.Outlined.CalendarMonth
            )
            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = address,
                onValueChange = { address = it },
                label = "Address",
                icon = Icons.Outlined.PinDrop,
                singleLine = false,
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = fatherName,
                onValueChange = { fatherName = it },
                label = "Father name",
                icon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words
            )
            Spacer(modifier = Modifier.height(12.dp))
            UploadField(
                value = gender,
                onValueChange = { gender = it },
                label = "Gender",
                icon = Icons.Outlined.Transgender
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onUpload(
                        UploadDocumentForm(
                            documentType = documentType,
                            documentName = documentName,
                            documentNumber = documentNumber,
                            issuedBy = issuedBy,
                            fullName = fullName,
                            dateOfBirth = dateOfBirth,
                            address = address,
                            fatherName = fatherName,
                            gender = gender
                        )
                    )
                },
                enabled = !isExtracting && documentNumber.isNotBlank() && documentName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Upload")
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
            // In dark theme, `surface` can blend into the background. `surfaceVariant`
            // gives a subtle contrast while staying Material 3.
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
            Column {
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
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = doc.issuedDate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
