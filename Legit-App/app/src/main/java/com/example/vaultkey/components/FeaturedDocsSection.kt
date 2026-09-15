package com.example.vaultkey.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vaultkey.data.DocumentSummaryDto

data class FeaturedDoc(
    val id: String?,
    val name: String,
    val type: String?,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun FeaturedDocsSection(
    documents: List<DocumentSummaryDto>,
    onDocClick: (DocumentSummaryDto) -> Unit,
    onAllDocsClick: () -> Unit
) {
    val displayedDocs = documents.map { doc ->
        val (icon, containerColor, contentColor) = getDocStyle(doc.documentType)
        FeaturedDoc(
            id = doc.id,
            name = doc.documentName,
            type = doc.documentType,
            icon = icon,
            containerColor = containerColor,
            contentColor = contentColor
        )
    }.toMutableList()

    // Always add "All Documents" at the end
    displayedDocs.add(
        FeaturedDoc(
            id = null,
            name = "All Documents",
            type = "ALL",
            icon = Icons.Rounded.GridView,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("My Vault")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(displayedDocs) { doc ->
                DocCard(doc, onClick = {
                    if (doc.type == "ALL") {
                        onAllDocsClick()
                    } else {
                        val originalDoc = documents.find { it.id == doc.id }
                        if (originalDoc != null) {
                            onDocClick(originalDoc)
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun getDocStyle(type: String): Triple<ImageVector, Color, Color> {
    return when (type) {
        "AADHAAR_CARD" -> Triple(
            Icons.Rounded.Badge,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        "PAN_CARD" -> Triple(
            Icons.Rounded.CreditCard,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
        "PASSPORT" -> Triple(
            Icons.Rounded.Public,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
        "DRIVING_LICENSE" -> Triple(
            Icons.Rounded.DirectionsCar,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "VOTER_ID" -> Triple(
            Icons.Rounded.HowToVote,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        "BANK_STATEMENT" -> Triple(
            Icons.Rounded.AccountBalance,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        "EDUCATION_CERTIFICATE" -> Triple(
            Icons.Rounded.School,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        "INCOME_PROOF" -> Triple(
            Icons.Rounded.Payments,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
        "ADDRESS_PROOF" -> Triple(
            Icons.Rounded.HistoryEdu,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
        else -> Triple(
            Icons.Rounded.Description,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocCard(doc: FeaturedDoc, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(120.dp),
        shape = RoundedCornerShape(32.dp),
        color = doc.containerColor,
        contentColor = doc.contentColor
    ) {
        SubtlePattern(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f))
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(

                shape = RoundedCornerShape(12.dp),
                color = doc.contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = doc.icon,
                        contentDescription = null,
                        tint = doc.contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = doc.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
