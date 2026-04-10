package io.github.howshous.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.howshous.data.models.ListingReview
import io.github.howshous.ui.theme.ReviewGreen
import io.github.howshous.ui.theme.ReviewRed
import io.github.howshous.ui.theme.TenantGreen
import kotlinx.coroutines.launch

@Composable
fun ReviewSubmissionSheet(
    existingReview: ListingReview?,
    onSubmit: (recommended: Boolean, comment: String) -> Unit,
    onUpdate: (recommended: Boolean, comment: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var recommended by remember { mutableStateOf(existingReview?.recommended ?: true) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val isEditMode = existingReview != null
    val scrollState = rememberScrollState()

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete your review? This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            onDelete()
                            showDeleteConfirm = false
                            isSubmitting = false
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFB00020))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            if (isEditMode) "Edit Your Review" else "Leave a Review",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        // Recommendation Toggle
        Text(
            "Would you recommend this listing?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecommendationToggleButton(
                label = "Recommend",
                isSelected = recommended,
                onClick = { recommended = true },
                color = ReviewGreen,
                modifier = Modifier.weight(1f)
            )
            RecommendationToggleButton(
                label = "Not Recommended",
                isSelected = !recommended,
                onClick = { recommended = false },
                color = ReviewRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Comment Field
        Text(
            "Add a comment (optional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        
        TextField(
            value = comment,
            onValueChange = { 
                if (it.length <= 500) {
                    comment = it
                }
            },
            placeholder = { Text("Share your experience with this listing...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        
        // Character count
        Text(
            "${comment.length}/500",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )

        if (errorMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                errorMessage,
                color = Color(0xFFB00020),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isSubmitting && !isLoading,
                    modifier = Modifier.weight(0.15f)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete review",
                        tint = Color(0xFFB00020)
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        errorMessage = ""
                        isSubmitting = true
                        if (isEditMode) {
                            onUpdate(recommended, comment)
                        } else {
                            onSubmit(recommended, comment)
                        }
                        isSubmitting = false
                    }
                },
                modifier = if (isEditMode) {
                    Modifier.weight(0.85f)
                } else {
                    Modifier.fillMaxWidth()
                },
                enabled = !isSubmitting && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TenantGreen,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isSubmitting || isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isEditMode) "Update Review" else "Submit Review")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RecommendationToggleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
