@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vaultkey.screens

import android.Manifest
import android.R.attr.progress
import android.net.Uri
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vaultkey.data.AppController
import com.example.vaultkey.ml.ThreatDetectionModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import java.util.Locale
import java.util.regex.Pattern

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    appController: AppController
) {
    val context = LocalContext.current
    var blockedPrediction by remember { mutableStateOf<ThreatDetectionModel.Prediction?>(null) }
    
    val threatModel = remember { ThreatDetectionModel(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            threatModel.close()
        }
    }

    val message = appController.message
    LaunchedEffect(message) {
        if (message != null) {
            if (message.contains("BLOCK", ignoreCase = true)) {
                blockedPrediction = blockedPrediction ?: ThreatDetectionModel.Prediction(
                    threatClass = "security_concern",
                    confidence = 1f,
                    isMalicious = true,
                    analyzedUrl = message.substringAfter('\n', ""),
                    maliciousScore = 1f
                )
            }
            appController.consumeMessage()
        }
    }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var scanResetTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(scanResetTrigger) {
        if (scanResetTrigger) {
            kotlinx.coroutines.delay(100)
            scanResetTrigger = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("THE DEDOX SCANNER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    val statusColor = if (threatModel.isInitialized) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Text(
                                if (threatModel.isInitialized) "SHIELDNET" else "HEURISTIC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                        }
                    }
                }
                )
                }
                ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (!hasCameraPermission) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission is required", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
                } else {
                // Camera View
                QRScannerView(
                    modifier = Modifier.fillMaxSize(),
                    triggerScanReset = scanResetTrigger,
                    onScanResult = { result ->
                        val prediction = threatModel.predict(result)
                        val contractId = extractContractId(result, prediction.analyzedUrl)

                        Log.d("ShieldNetDemo", "Scanned: $result")
                        Log.d("ShieldNetDemo", "Analyzed: ${prediction.analyzedUrl}")
                        Log.d("ShieldNetDemo", "Malicious: ${prediction.isMalicious}, Class: ${prediction.threatClass}")

                        // 1. Check for malicious block FIRST
                        if (prediction.isMalicious) {
                            blockedPrediction = prediction
                            true
                        } 

                        // 2. Then check for valid contract ID
                        else if (contractId != null) {
                            appController.fetchScannedContract(
                                contractId, 
                                onSuccess = onBack,
                                onFailure = { 
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to fetch contract: ${appController.message ?: "Unknown error"}")
                                        kotlinx.coroutines.delay(2000)
                                        scanResetTrigger = true 
                                    }
                                }
                            )
                            true
                        } 
                        // 3. Handle safe links that aren't contracts
                        else {
                            val displayUrl = prediction.analyzedUrl.take(30) + if (prediction.analyzedUrl.length > 30) "..." else ""
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = "Safe link (no contract): $displayUrl",
                                    duration = SnackbarDuration.Short
                                )
                                kotlinx.coroutines.delay(2000)
                                scanResetTrigger = true
                            }
                            true
                        }
                    }
                )

                // Scanning Box Overlay
                ScannerOverlay(modifier = Modifier.fillMaxSize())

                // Manual Entry + Text instruction
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 40.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        // Manual UID Entry Field
                        var manualId by remember { mutableStateOf("") }
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualId,
                                    onValueChange = { if (it.length <= 24) manualId = it },
                                    placeholder = { Text("Manual Contract UID", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                                )
                                Button(
                                    onClick = {
                                        if (manualId.length == 24) {
                                            appController.fetchScannedContract(
                                                manualId,
                                                onSuccess = onBack,
                                                onFailure = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Invalid UID or Contract not found")
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    enabled = manualId.length == 24 && !appController.loading,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    if (appController.loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("GET")
                                    }
                                }
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                "ALIGN QR CODE WITHIN THE BOX",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                }

                // Security Alert (Using Dialog to ensure it sits above the Camera Layer)
                blockedPrediction?.let { prediction ->
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { 
                            blockedPrediction = null 
                            scanResetTrigger = true
                        },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = false
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 10.dp,
                                shadowElevation = 20.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.GppBad,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Security Concern",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = "ShieldNet ML Block",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = prediction.threatClass.uppercase(Locale.ROOT),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "${(prediction.confidence * 100).toInt()}%",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }

                                    Text(
                                        text = "This scan was intercepted. The destination URL exhibits high-risk patterns associated with phishing.",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Analyzed Target", style = MaterialTheme.typography.labelLarge)
                                            Text(
                                                text = prediction.analyzedUrl,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { 
                                            blockedPrediction = null 
                                            scanResetTrigger = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("DISMISS AND RESET", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                }
                }

}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.colorScheme.primary
    val scrimColor = Color.Black.copy(alpha = 0.5f)

    Canvas(modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val width = size.width
        val height = size.height
        val boxSize = size.width * 0.7f
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2

        // Draw the scrim
        drawRect(color = scrimColor)
        
        // Punch the hole
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24f),
            blendMode = BlendMode.Clear
        )

        // Draw the border
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24f),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
fun QRScannerView(
    modifier: Modifier = Modifier,
    triggerScanReset: Boolean = false,
    onScanResult: (String) -> Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isScanned by remember { mutableStateOf(false) }

    LaunchedEffect(triggerScanReset) {
        if (triggerScanReset) {
            isScanned = false
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var scanner: BarcodeScanner? by remember { mutableStateOf(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val barcodeScanner = BarcodeScanning.getClient()
                scanner = barcodeScanner

                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    if (isScanned) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let { result ->
                                    if (onScanResult(result)) {
                                        isScanned = true
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
                } catch (e: Exception) {
                    Log.e("QRScanner", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        onRelease = {
            try {
                scanner?.close()
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("QRScanner", "Cleanup failed", e)
            }
        }
    )
}

private val mongoObjectIdPattern = Pattern.compile("^[a-fA-F0-9]{24}$")

private fun extractContractId(rawValue: String, analyzedValue: String): String? {
    return sequenceOf(rawValue, analyzedValue)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { candidate -> extractContractIdFromCandidate(candidate) }
        .firstOrNull()
}

private fun extractContractIdFromCandidate(candidate: String): String? {
    if (mongoObjectIdPattern.matcher(candidate).matches()) {
        return candidate
    }

    val parsed = try {
        Uri.parse(candidate)
    } catch (_: Exception) {
        null
    } ?: return null

    val queryParamId = listOf("contractId", "contract_id", "id")
        .firstNotNullOfOrNull { key -> parsed.getQueryParameter(key)?.trim() }
        ?.takeIf { mongoObjectIdPattern.matcher(it).matches() }
    if (queryParamId != null) {
        return queryParamId
    }

    val pathSegments = parsed.pathSegments.orEmpty()
    val verifyIndex = pathSegments.indexOfLast { it.equals("verify", ignoreCase = true) }
    if (verifyIndex != -1 && verifyIndex + 1 < pathSegments.size) {
        val nextSegment = pathSegments[verifyIndex + 1].trim()
        if (mongoObjectIdPattern.matcher(nextSegment).matches()) {
            return nextSegment
        }
    }

    val lastSegment = pathSegments.lastOrNull()?.trim().orEmpty()
    return lastSegment.takeIf { mongoObjectIdPattern.matcher(it).matches() }
}
