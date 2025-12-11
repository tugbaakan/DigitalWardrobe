package com.digitalwardrobe.ui.screens.garment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.digitalwardrobe.data.models.Garment
import com.digitalwardrobe.data.models.GarmentColor
import com.digitalwardrobe.data.models.GarmentFit
import com.digitalwardrobe.data.models.GarmentFormality
import com.digitalwardrobe.data.models.GarmentType
import com.digitalwardrobe.data.repository.FirestoreRepository
import com.digitalwardrobe.data.repository.StorageRepository
import com.digitalwardrobe.ui.theme.DigitalWardrobeTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentDetailScreen(
    garmentId: String,
    onBackClick: () -> Unit,
    onGarmentDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val firestoreRepository = remember { FirestoreRepository() }
    val storageRepository = remember { StorageRepository() }

    var garment by remember { mutableStateOf<Garment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Form state
    var selectedType by remember { mutableStateOf(GarmentType.SHIRT) }
    var selectedColor by remember { mutableStateOf(GarmentColor.BLACK) }
    var selectedFormality by remember { mutableStateOf(GarmentFormality.CASUAL) }
    var selectedFit by remember { mutableStateOf(GarmentFit.REGULAR) }
    var description by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Load garment when screen is first displayed
    LaunchedEffect(garmentId) {
        loadGarment(
            garmentId = garmentId,
            firestoreRepository = firestoreRepository,
            onSuccess = { loadedGarment ->
                garment = loadedGarment
                // Initialize form with current values
                selectedType = loadedGarment.type
                selectedColor = loadedGarment.color
                selectedFormality = loadedGarment.formality
                selectedFit = loadedGarment.fit
                description = loadedGarment.description ?: ""
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Garment",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this garment? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            deleteGarment(
                                garment!!,
                                firestoreRepository = firestoreRepository,
                                storageRepository = storageRepository,
                                onSuccess = { onGarmentDeleted() },
                                onError = { error -> saveError = error }
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Garment" else "Garment Details",
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
                },
                actions = {
                    if (!isLoading && garment != null) {
                        if (isEditMode) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        saveGarmentChanges(
                                            garmentId = garmentId,
                                            metadata = GarmentMetadata(
                                                type = selectedType,
                                                color = selectedColor,
                                                formality = selectedFormality,
                                                fit = selectedFit,
                                                description = description.takeIf { it.isNotBlank() }
                                            ),
                                            firestoreRepository = firestoreRepository,
                                            onSuccess = {
                                                isEditMode = false
                                                isSaving = false
                                                // Reload garment data
                                                scope.launch {
                                                    loadGarment(
                                                        garmentId = garmentId,
                                                        firestoreRepository = firestoreRepository,
                                                        onSuccess = { loadedGarment ->
                                                            garment = loadedGarment
                                                        },
                                                        onError = { /* Handle error silently */ }
                                                    )
                                                }
                                            },
                                            onError = { error ->
                                                isSaving = false
                                                saveError = error
                                            }
                                        )
                                    }
                                },
                                enabled = !isSaving
                            ) {
                                Text("Save")
                            }
                        } else {
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit garment"
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete garment",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load garment",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                loadGarment(
                                    garmentId = garmentId,
                                    firestoreRepository = firestoreRepository,
                                    onSuccess = { loadedGarment ->
                                        garment = loadedGarment
                                        selectedType = loadedGarment.type
                                        selectedColor = loadedGarment.color
                                        selectedFormality = loadedGarment.formality
                                        selectedFit = loadedGarment.fit
                                        description = loadedGarment.description ?: ""
                                        isLoading = false
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                        isLoading = false
                                    }
                                )
                            }
                        }) {
                            Text("Try Again")
                        }
                    }
                }
                garment != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Garment Image
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(0.75f),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(garment!!.imageUrl),
                                contentDescription = "${garment!!.type} ${garment!!.color}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Metadata Form
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
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
                                    valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                                    enabled = isEditMode
                                )

                                // Color
                                MetadataDropdown(
                                    label = "Color",
                                    selectedValue = selectedColor,
                                    options = GarmentColor.entries,
                                    onValueChange = { selectedColor = it },
                                    valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                                    enabled = isEditMode
                                )

                                // Formality
                                MetadataDropdown(
                                    label = "Formality",
                                    selectedValue = selectedFormality,
                                    options = GarmentFormality.entries,
                                    onValueChange = { selectedFormality = it },
                                    valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                                    enabled = isEditMode
                                )

                                // Fit
                                MetadataDropdown(
                                    label = "Fit/Cut",
                                    selectedValue = selectedFit,
                                    options = GarmentFit.entries,
                                    onValueChange = { selectedFit = it },
                                    valueToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                                    enabled = isEditMode
                                )

                                // Description
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Description (Optional)") },
                                    placeholder = { Text("e.g., Favorite summer shirt, good for casual outings") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4,
                                    readOnly = !isEditMode
                                )

                                // Metadata info (when not editing)
                                if (!isEditMode) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Added: ${garment!!.createdAt?.toString() ?: "Unknown"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Save Error
                        saveError?.let { error ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valueToString(selectedValue),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled,
            colors = if (enabled) {
                OutlinedTextFieldDefaults.colors()
            } else {
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        if (enabled) {
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
}

/**
 * Function to load a garment from Firestore.
 */
suspend fun loadGarment(
    garmentId: String,
    firestoreRepository: FirestoreRepository,
    onSuccess: (Garment) -> Unit,
    onError: (String) -> Unit
) {
    try {
        when (val result = firestoreRepository.getGarment(garmentId)) {
            is FirestoreRepository.DatabaseResult.Success -> {
                result.data?.let { onSuccess(it) } ?: onError("Garment not found")
            }
            is FirestoreRepository.DatabaseResult.Error -> {
                onError("Failed to load garment: ${result.exception.message}")
            }
        }
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
    }
}

/**
 * Function to save garment changes to Firestore.
 */
suspend fun saveGarmentChanges(
    garmentId: String,
    metadata: GarmentMetadata,
    firestoreRepository: FirestoreRepository,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val updates = mutableMapOf<String, Any>(
            "type" to metadata.type,
            "color" to metadata.color,
            "formality" to metadata.formality,
            "fit" to metadata.fit
        )

        // Only add description if it's not null
        metadata.description?.let {
            updates["description"] = it
        }

        when (val result = firestoreRepository.updateGarment(garmentId, updates)) {
            is FirestoreRepository.DatabaseResult.Success -> {
                onSuccess()
            }
            is FirestoreRepository.DatabaseResult.Error -> {
                onError("Failed to save changes: ${result.exception.message}")
            }
        }
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
    }
}

/**
 * Function to delete a garment from Firestore and Storage.
 */
suspend fun deleteGarment(
    garment: Garment,
    firestoreRepository: FirestoreRepository,
    storageRepository: StorageRepository,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Delete from Firestore first
        when (val firestoreResult = firestoreRepository.deleteGarment(garment.id)) {
            is FirestoreRepository.DatabaseResult.Success -> {
                // Delete from Storage
                storageRepository.deleteFile(garment.imageUrl)
                onSuccess()
            }
            is FirestoreRepository.DatabaseResult.Error -> {
                onError("Failed to delete garment: ${firestoreResult.exception.message}")
            }
        }
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GarmentDetailScreenPreview() {
    DigitalWardrobeTheme {
        GarmentDetailScreen(
            garmentId = "sample_id",
            onBackClick = {},
            onGarmentDeleted = {}
        )
    }
}
