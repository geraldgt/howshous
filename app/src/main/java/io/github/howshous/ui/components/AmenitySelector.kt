package io.github.howshous.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.howshous.data.firestore.AmenityRepository
import kotlinx.coroutines.launch

@Composable
fun AmenitySelector(
    selectedAmenities: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    allowCustomAmenities: Boolean = false,
    userId: String = "",
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedLabelColor: Color = MaterialTheme.colorScheme.onPrimary,
    onError: (String) -> Unit = {},
) {
    val amenityRepository = remember { AmenityRepository() }
    val scope = rememberCoroutineScope()
    var availableAmenities by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var customAmenityInput by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    fun refreshAmenities() {
        scope.launch {
            isLoading = true
            availableAmenities = amenityRepository.getAvailableAmenities()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAmenities()
    }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableAmenities) { amenity ->
                            FilterChip(
                                selected = selectedAmenities.contains(amenity),
                                onClick = {
                                    onSelectionChange(
                                        if (selectedAmenities.contains(amenity)) {
                                            selectedAmenities - amenity
                                        } else {
                                            selectedAmenities + amenity
                                        }
                                    )
                                },
                                label = { Text(amenity, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = selectedContainerColor,
                                    selectedLabelColor = selectedLabelColor,
                                    selectedTrailingIconColor = selectedLabelColor,
                                    selectedLeadingIconColor = selectedLabelColor
                                )
                            )
                        }
                    }
                }
            }
        }

        if (allowCustomAmenities) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customAmenityInput,
                    onValueChange = { customAmenityInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Add custom amenity") },
                    placeholder = { Text("e.g. Generator backup") },
                    singleLine = true,
                    enabled = !isAdding
                )
                Button(
                    onClick = {
                        if (customAmenityInput.isBlank() || userId.isBlank() || isAdding) return@Button
                        isAdding = true
                        scope.launch {
                            val result = amenityRepository.createAmenity(customAmenityInput, userId)
                            isAdding = false
                            result.onSuccess { label ->
                                customAmenityInput = ""
                                refreshAmenities()
                                onSelectionChange(selectedAmenities + label)
                            }.onFailure { error ->
                                onError(error.message ?: "Could not add amenity.")
                            }
                        }
                    },
                    enabled = customAmenityInput.isNotBlank() && userId.isNotBlank() && !isAdding
                ) {
                    Text(if (isAdding) "..." else "Add")
                }
            }
        }
    }
}
