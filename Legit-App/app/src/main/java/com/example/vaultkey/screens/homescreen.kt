package com.example.vaultkey.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.vaultkey.data.AppController
import com.example.vaultkey.components.FeaturedDocsSection
import com.example.vaultkey.components.LegitTopBar
import com.example.vaultkey.components.PipelineStepCard
import com.example.vaultkey.components.PrivacyBanner
import com.example.vaultkey.components.SectionHeader
import com.example.vaultkey.components.WelcomeCard
import com.example.vaultkey.components.VerificationSheetContent
import com.example.vaultkey.components.ScannedContractSheetContent
import com.example.vaultkey.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    appController: AppController,
    onProfileClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // State to track which sheet to show for documents or other info
    var activeSheet by remember { mutableStateOf<SheetType?>(null) }

    var showText by remember { mutableStateOf(true) }

    val message = appController.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            appController.consumeMessage()
        }
    }

    // Observe scanned contract from AppController
    LaunchedEffect(appController.scannedContract) {
        if (appController.scannedContract != null) {
            activeSheet = SheetType.SCANNED_CONTRACT
        }
    }

    LaunchedEffect(Unit) {
        delay(5000)
        showText = false
    }

    // Entry animation state
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        appController.refreshAll()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LegitTopBar(
                scrollBehavior = scrollBehavior,
                onNotificationClick = { navController.navigate("qr") },
                onVerificationClick = { activeSheet = SheetType.VERIFICATIONS },
                onProfileClick = onProfileClick
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
//                    FloatingActionButton(
//                        onClick = {}
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .padding(12.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                imageVector = ImageVector.vectorResource(R.drawable.sparkles),
//                                contentDescription = null
//                            )
//                            //Spacer(Modifier.width(10.dp))
//                            AnimatedVisibility(
//                                visible = showText,
//                                enter = fadeIn(
//                                    animationSpec = tween(
//                                        durationMillis = 300,
//                                        easing = FastOutSlowInEasing
//                                    )
//                                ) + slideInHorizontally(
//                                    initialOffsetX = { it / 2 },
//                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
//                                ),
//                                exit = fadeOut(
//                                    animationSpec = tween(
//                                        durationMillis = 250,
//                                        easing = LinearOutSlowInEasing
//                                    )
//                                ) + slideOutHorizontally(
//                                    targetOffsetX = { it / 2 },
//                                    animationSpec = tween(250, easing = LinearOutSlowInEasing)
//                                )
//                            ) {
//                                Row {
//                                    Spacer(Modifier.width(10.dp))
//                                    Text("Chat with AI")
//                                }
//                            }
//                        }
//                    }
//                    Spacer(Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = {
                            navController.navigate("verification")
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Verifications",
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                AnimatedEntryItem(visible = visible, index = 0) {
                    WelcomeCard(
                        name = appController.profile?.fullName
                            ?: appController.session?.username
                            ?: "Vaultkey User"
                    )
                }
            }

            item {
                AnimatedEntryItem(visible = visible, index = 1) {
                    PrivacyBanner()
                }
            }

            item {
                AnimatedEntryItem(visible = visible, index = 2) {
                    FeaturedDocsSection(
                        documents = appController.documents,
                        onDocClick = { doc ->
                            appController.fetchDocumentDetails(doc.id)
                            activeSheet = SheetType.DOCUMENT_PREVIEW
                        },
                        onAllDocsClick = { navController.navigate("issued") }
                    )
                }
            }

            item {
                AnimatedEntryItem(visible = visible, index = 3) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = "Verification Pipeline")
                        PipelineStepCard(
                            title = "Pending Contracts",
                            status = appController.pendingContracts.size.toString(),
                            timestamp = "Requests waiting for your action",
                            icon = Icons.Rounded.CheckCircle,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        PipelineStepCard(
                            title = "Vault Documents",
                            status = appController.documents.size.toString(),
                            timestamp = "Synced from The Dedox backend",
                            icon = Icons.Rounded.Refresh,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        PipelineStepCard(
                            title = "Account Role",
                            status = appController.session?.role ?: "USER",
                            timestamp = appController.profile?.email ?: "Backend connected",
                            icon = Icons.Rounded.Lock,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Bottom Sheet Logic
        activeSheet?.let { sheetType ->
            ModalBottomSheet(
                onDismissRequest = { 
                    activeSheet = null
                    appController.clearCurrentDocument()
                    appController.clearScannedContract()
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                when (sheetType) {
                    SheetType.NOTIFICATIONS -> NotificationSheetContent()
                    SheetType.VERIFICATIONS -> VerificationSheetContent(appController)
                    SheetType.SCANNED_CONTRACT -> {
                        ScannedContractSheetContent(
                            appController = appController,
                            onDismiss = {
                                activeSheet = null
                                appController.clearScannedContract()
                            },
                            onSuccess = {
                                // Already handled in sheet content if needed, 
                                // but we might want to refresh things here
                                appController.refreshPendingContracts()
                            }
                        )
                    }
                    SheetType.DOCUMENT_PREVIEW -> {
                        val doc = appController.currentDocument
                        if (doc != null) {
                            DocumentViewSheet(
                                docType = doc.documentType,
                                docTitle = doc.documentName,
                                holderName = doc.metadata.fullName ?: appController.profile?.fullName,
                                docNumberMasked = doc.documentNumber, // Backend masks it appropriately in response
                                holderDob = doc.metadata.dateOfBirth,
                                onDismiss = { 
                                    activeSheet = null
                                    appController.clearCurrentDocument()
                                }
                            )
                        } else if (appController.loading) {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        }
                    }
                    SheetType.DOC_AADHAR -> {
                        // Legacy support if needed, but DOCUMENT_PREVIEW is preferred
                    }
                    SheetType.DOC_PAN -> {
                        // Legacy support if needed
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No new notifications",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "We'll notify you when someone requests a document verification.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun AnimatedEntryItem(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    val animationDelay = index * 100
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = animationDelay),
        label = "alpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "translateY"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(
                alpha = alpha,
                translationY = translateY
            )
    ) {
        content()
    }
}

enum class SheetType {
    NOTIFICATIONS,
    VERIFICATIONS,
    DOCUMENT_PREVIEW,
    SCANNED_CONTRACT,
    DOC_AADHAR,
    DOC_PAN
}
