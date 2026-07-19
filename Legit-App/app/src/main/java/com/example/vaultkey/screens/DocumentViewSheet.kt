package com.example.vaultkey.screens


import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun DocumentViewSheet(
    docType: String,
    docTitle: String? = null,
    holderName: String? = null,
    docNumberMasked: String? = null,
    holderDob: String? = null,
    onDismiss: () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context.findActivity()
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification")
            .setSubtitle("Authenticate to view your $docType card")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()
    }

    val biometricPrompt = remember(activity) {
        activity?.let {
            BiometricPrompt(it, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    val prettyType = when {
        docType.equals("Aadhar", ignoreCase = true) || docType.contains("AADHAAR", ignoreCase = true) -> "Aadhaar"
        docType.equals("PAN", ignoreCase = true) || docType.contains("PAN", ignoreCase = true) -> "PAN"
        else -> docTitle ?: docType.replace('_', ' ')
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isUnlocked) {
            SecurityLockContent(onUnlock = {
                val biometricManager = BiometricManager.from(context)
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                
                when (biometricManager.canAuthenticate(authenticators)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        if (biometricPrompt != null) {
                            biometricPrompt.authenticate(promptInfo)
                        } else {
                            Toast.makeText(context, "Authentication error: Prompt not initialized", Toast.LENGTH_SHORT).show()
                        }
                    }
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                        Toast.makeText(context, "No biometric features available on this device", Toast.LENGTH_SHORT).show()
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                        Toast.makeText(context, "Biometric features are currently unavailable", Toast.LENGTH_SHORT).show()
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                        Toast.makeText(context, "Please enroll biometrics in your device settings", Toast.LENGTH_SHORT).show()
                    else ->
                        Toast.makeText(context, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            DocHeader(prettyType)
            
            Spacer(modifier = Modifier.height(8.dp))

            when {
                prettyType == "Aadhaar" -> AadharCardContent(
                    holderName = holderName,
                    dateOfBirth = holderDob,
                    maskedNumber = docNumberMasked
                )
                prettyType == "PAN" -> PanCardContent(
                    holderName = holderName,
                    dateOfBirth = holderDob,
                    maskedNumber = docNumberMasked
                )
                else -> {
                    Text(
                        text = "Preview not available for this document type yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* TODO: Share Proof */ },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Share Cryptographic Proof", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun DocHeader(title: String) {
    Text(
        text = "$title Card",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SecurityLockContent(onUnlock: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Access Restricted",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            "For your security, please authenticate to reveal this sensitive document.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Unlock with Biometrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AadharCardContent(
    holderName: String?,
    dateOfBirth: String?,
    maskedNumber: String?
) {
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
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(holderName?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("DOB: ${dateOfBirth?.ifBlank { "—" } ?: "—"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("GENDER: Male / पुरुष", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                        
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
                    text = maskedNumber?.ifBlank { "XXXX XXXX XXXX" } ?: "XXXX XXXX XXXX",
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
fun PanCardContent(
    holderName: String?,
    dateOfBirth: String?,
    maskedNumber: String?
) {
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
                for (i in 0..size.width.toInt() step 40) {
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
                        Text(holderName?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("FATHER'S NAME / पिता का नाम", fontSize = 8.sp, color = Color.Black)
                        Text("XXXX XXXX", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("DATE OF BIRTH / जन्म तिथि", fontSize = 8.sp, color = Color.Black)
                        Text(dateOfBirth?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("स्थायी लेखा संख्या कार्ड", fontSize = 8.sp, color = Color.Black)
                        Text("Permanent Account Number Card", fontSize = 8.sp, color = Color.Black)
                        Text(
                            text = maskedNumber?.ifBlank { "—" } ?: "—",
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
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color(0xFF004D40).copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
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
