package com.example.vaultkey.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vaultkey.data.AppController
import com.example.vaultkey.data.VerificationResponseDto

@Composable
fun ScannedContractSheetContent(
    appController: AppController,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val contractDetails = appController.scannedContract
    val isLoading = appController.loading
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Verification Request",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (contractDetails != null) {
            // Premium Verification Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val decorationColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.6f)
                            quadraticBezierTo(size.width * 0.4f, size.height * 0.4f, size.width, size.height * 0.7f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = decorationColor)

                        for (i in 0..size.width.toInt() step 60) {
                            drawLine(
                                color = decorationColor,
                                start = Offset(i.toFloat(), 0f),
                                end = Offset(i.toFloat() + 40f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (contractDetails.metadata?.fullName?.firstOrNull() ?: "?").toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contractDetails.metadata?.fullName ?: "Verified User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "SECURE VERIFICATION PIPELINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PURPOSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
                                Text(
                                    contractDetails.purpose ?: "Identity Check",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            }
                            Icon(
                                Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Requirements Section Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RequirementColumn(
                        title = "DATA FIELDS",
                        items = contractDetails.requiredFields ?: emptyList(),
                        modifier = Modifier.weight(1f)
                    )
                    RequirementColumn(
                        title = "DOCUMENTS",
                        items = contractDetails.requiredDocumentTypes ?: emptyList(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "INITIALIZING SECURE PIPELINE...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            Button(
                onClick = {
                    contractDetails?.contractId?.let { id ->
                        appController.approveContract(id, true, onSuccess = {
                            Toast.makeText(context, "Verification approved successfully", Toast.LENGTH_SHORT).show()
                            onSuccess()
                            onDismiss()
                        })
                    }
                },
                enabled = contractDetails != null,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("APPROVE & VERIFY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    contractDetails?.contractId?.let { id ->
                        appController.approveContract(id, false, onSuccess = {
                            Toast.makeText(context, "Contract rejected", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        })
                    } ?: onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "DECLINE REQUEST",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RequirementColumn(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }
        }
    }
}
