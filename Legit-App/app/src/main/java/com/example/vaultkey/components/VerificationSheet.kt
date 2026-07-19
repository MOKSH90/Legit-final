package com.example.vaultkey.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vaultkey.data.AppController

data class VerificationRequest(
    val id: Int,
    val requester: String,
    val documentType: String,
    val time: String,
    val icon: ImageVector = Icons.Rounded.Description
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationSheetContent(
    appController: AppController
) {
    val requests = appController.pendingContracts
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Pending Verifications",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Text(
            text = "Swipe Right to Approve • Swipe Left to Decline",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests, key = { it.contractId }) { contract ->
                val request = VerificationRequest(
                    id = contract.contractId.hashCode(),
                    requester = contract.requesterName,
                    documentType = contract.requiredDocumentTypes.joinToString(),
                    time = "Pending"
                )
                SwipeableVerificationItem(
                    request = request,
                    onDismiss = { direction ->
                        val approved = direction == SwipeToDismissBoxValue.StartToEnd
                        appController.approveContract(contract.contractId, approved, onSuccess = {
                            val msg = if (approved) "Verification approved successfully" else "Contract rejected"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        })
                    }
                )
            }
        }

        if (requests.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("All caught up!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableVerificationItem(
    request: VerificationRequest,
    onDismiss: (SwipeToDismissBoxValue) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss(it)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {

            val direction = dismissState.dismissDirection

            val color by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "dismissColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 20.dp),
                contentAlignment =
                    if (direction == SwipeToDismissBoxValue.StartToEnd)
                        Alignment.CenterStart
                    else
                        Alignment.CenterEnd
            ) {

                when (direction) {

                    SwipeToDismissBoxValue.StartToEnd -> {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Approve",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    SwipeToDismissBoxValue.Settled -> {}
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(request.icon, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.requester, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(request.documentType, style = MaterialTheme.typography.bodyMedium)
                }
                Text(request.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}