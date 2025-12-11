package com.digitalwardrobe.ui.screens.garment

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.digitalwardrobe.data.models.*
import com.digitalwardrobe.data.repository.FirestoreRepository
import com.digitalwardrobe.data.repository.StorageRepository
import com.digitalwardrobe.ui.theme.DigitalWardrobeTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Function to save a garment with image upload and metadata storage.
 */
suspend fun saveGarment(
    imageUri: String,
    metadata: GarmentMetadata,
    storageRepository: StorageRepository,
    firestoreRepository: FirestoreRepository,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError("User not authenticated")
            return
        }

        val userId = currentUser.uid

        // Upload image to Firebase Storage
        val imageUriObj = Uri.parse(imageUri)
        val uploadFlow = storageRepository.uploadGarmentPhoto(userId, imageUriObj)

        var downloadUrl: String? = null
        uploadFlow.collect { result ->
            when (result) {
                is StorageRepository.UploadResult.Progress -> {
                    // Could show progress here if needed
                }
                is StorageRepository.UploadResult.Success -> {
                    downloadUrl = result.downloadUrl
                }
                is StorageRepository.UploadResult.Error -> {
                    onError("Failed to upload image: ${result.exception.message}")
                    return@collect
                }
            }
        }

        if (downloadUrl == null) {
            onError("Failed to get download URL")
            return
        }

        // Create garment object
        val garment = Garment(
            id = "", // Will be set by Firestore
            userId = userId,
            imageUrl = downloadUrl!!,
            type = metadata.type,
            color = metadata.color,
            formality = metadata.formality,
            fit = metadata.fit,
            description = metadata.description
        )

        // Save to Firestore
        when (val result = firestoreRepository.saveGarment(garment)) {
            is FirestoreRepository.DatabaseResult.Success -> {
                onSuccess()
            }
            is FirestoreRepository.DatabaseResult.Error -> {
                onError("Failed to save garment: ${result.exception.message}")
            }
        }

    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentMetadataScreen(
    garmentImageUri: String,
    onBackClick: () -> Unit,
    onSaveGarment: (GarmentMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories
    val storageRepository = remember { StorageRepository() }
    val firestoreRepository = remember { FirestoreRepository() }

    var selectedType by remember { mutableStateOf(GarmentType.SHIRT) }
    var selectedColor by remember { mutableStateOf(GarmentColor.BLACK) }
    var selectedFormality by remember { mutableStateOf(GarmentFormality.CASUAL) }
    var selectedFit by remember { mutableStateOf(GarmentFit.REGULAR) }
    var description by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Garment Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📋 Add Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Help us categorize your garment by selecting the appropriate tags below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Metadata Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Garment Type
                    MetadataDropdown(
                        label = "Garment Type",
                        selectedValue = selectedType,
                        options = GarmentType.entries,
                        onValueChange = { selectedType = it },
                        valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )

                    // Color
                    MetadataDropdown(
                        label = "Color",
                        selectedValue = selectedColor,
                        options = GarmentColor.entries,
                        onValueChange = { selectedColor = it },
                        valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )

                    // Formality
                    MetadataDropdown(
                        label = "Formality",
                        selectedValue = selectedFormality,
                        options = GarmentFormality.entries,
                        onValueChange = { selectedFormality = it },
                        valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )

                    // Fit
                    MetadataDropdown(
                        label = "Fit/Cut",
                        selectedValue = selectedFit,
                        options = GarmentFit.entries,
                        onValueChange = { selectedFit = it },
                        valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("e.g., Favorite summer shirt, good for casual outings") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    isSaving = true
                    saveError = null
                    scope.launch {
                        saveGarment(
                            imageUri = garmentImageUri,
                            metadata = GarmentMetadata(
                                type = selectedType,
                                color = selectedColor,
                                formality = selectedFormality,
                                fit = selectedFit,
                                description = description.takeIf { it.isNotBlank() }
                            ),
                            storageRepository = storageRepository,
                            firestoreRepository = firestoreRepository,
                            onSuccess = {
                                isSaving = false
                                onBackClick() // Navigate back on success
                            },
                            onError = { error ->
                                isSaving = false
                                saveError = error
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Save Garment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error message
            saveError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Text
            Text(
                text = "You can edit these details later from your wardrobe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MetadataDropdown(
    label: String,
    selectedValue: T,
    options: List<T>,
    onValueChange: (T) -> Unit,
    valueToString: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valueToString(selectedValue),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(valueToString(option)) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

/**
 * Data class to hold garment metadata for easier passing between screens.
 */
data class GarmentMetadata(
    val type: GarmentType,
    val color: GarmentColor,
    val formality: GarmentFormality,
    val fit: GarmentFit,
    val description: String? = null
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GarmentMetadataScreenPreview() {
    DigitalWardrobeTheme {
        GarmentMetadataScreen(
            garmentImageUri = "sample_uri",
            onBackClick = {},
            onSaveGarment = {}
        )
    }
}
