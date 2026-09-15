package com.example.vaultkey.screens


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.vaultkey.data.DocumentResponseDto

enum class DocType { AADHAR, PAN, EVIDENCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewScreen(
    docType: DocType,
    document: DocumentResponseDto? = null,
    onBack: () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(document?.documentName ?: if (docType == DocType.AADHAR) "Aadhar Card" else if (docType == DocType.PAN) "PAN Card" else "Evidence Record", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { /* Share Proof */ }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share Proof")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                SecurityLockScreen(onUnlock = { isUnlocked = true })
            } else {
                when (docType) {
                    DocType.AADHAR -> AadharCardView(document)
                    DocType.PAN -> PanCardView(document)
                    DocType.EVIDENCE -> {
                        EvidenceCardContent(
                            docType = document?.documentType ?: "EVIDENCE_RECORD",
                            docTitle = document?.documentName ?: "Evidence Document",
                            docNumber = document?.documentNumber,
                            officerName = document?.metadata?.officerName ?: document?.metadata?.fullName,
                            dobOrDate = document?.metadata?.dateOfBirth
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityLockScreen(onUnlock: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Rounded.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Authentication Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Please use fingerprint to view secure document",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUnlock,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Authenticate", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AadharCardView(doc: DocumentResponseDto?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.7f)
                    quadraticBezierTo(size.width * 0.5f, size.height * 0.8f, size.width, size.height * 0.6f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = Color(0xFFFFF3E0))
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("भारतीय विशिष्ट पहचान प्राधिकरण", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        Text("Unique Identification Authority of India", fontSize = 8.sp, color = Color.Gray)
                    }
                    Text("AADHAR", fontWeight = FontWeight.Black, color = Color(0xFFD32F2F), fontSize = 18.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color(0xFFD32F2F).copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc?.metadata?.fullName?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("DOB: ${doc?.metadata?.dateOfBirth?.ifBlank { "—" } ?: "—"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("GENDER: ${doc?.metadata?.gender ?: "Male / पुरुष"}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                        
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFFD32F2F))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doc?.documentNumber ?: "XXXX XXXX XXXX",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = 4.sp,
                        lineHeight = 32.sp
                    ),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PanCardView(doc: DocumentResponseDto?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background texture
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..size.width.toInt() step(40)) {
                    drawLine(
                        color = Color(0xFF004D40).copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(i.toFloat(), 0f),
                        end = androidx.compose.ui.geometry.Offset(i.toFloat() + 100f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("आयकर विभाग", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text("INCOME TAX DEPARTMENT", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("भारत सरकार", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text("GOVT. OF INDIA", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NAME / नाम", fontSize = 8.sp, color = Color.Black)
                        Text(doc?.metadata?.fullName?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("FATHER'S NAME / पिता का नाम", fontSize = 8.sp, color = Color.Black)
                        Text(doc?.metadata?.fatherName?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("DATE OF BIRTH / जन्म तिथि", fontSize = 8.sp, color = Color.Black)
                        Text(doc?.metadata?.dateOfBirth?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("स्थायी लेखा संख्या कार्ड", fontSize = 8.sp, color = Color.Black)
                        Text("Permanent Account Number Card", fontSize = 8.sp, color = Color.Black)
                        Text(
                            text = doc?.documentNumber ?: "—",
                            style = MaterialTheme.typography.titleMedium.copy(
                                letterSpacing = 1.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color(0xFF004D40).copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF004D40).copy(alpha = 0.2f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(25.dp)
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SIGNATURE", fontSize = 8.sp, color = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
