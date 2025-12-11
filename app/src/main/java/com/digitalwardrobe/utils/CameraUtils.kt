package com.digitalwardrobe.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

/**
 * Camera utilities for capturing photos.
 */
object CameraUtils {

    /**
     * Check if camera permission is granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Create a temporary image file for camera capture.
     */
    fun createImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date())
        val storageDir: File = context.cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Get URI for a file using FileProvider.
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

/**
 * Camera capture state.
 */
sealed class CameraState {
    data object Ready : CameraState()
    data object Capturing : CameraState()
    data class Success(val uri: Uri) : CameraState()
    data class Error(val exception: Exception) : CameraState()
}

/**
 * Simple camera permission and capture state holder.
 */
class CameraCaptureState {
    var hasPermission by mutableStateOf(false)
        internal set

    var isCapturing by mutableStateOf(false)
        internal set

    var currentError by mutableStateOf<Exception?>(null)
        internal set

    lateinit var requestPermission: () -> Unit
        internal set

    fun capturePhoto(onPhotoCaptured: (Uri) -> Unit) {
        if (!hasPermission) {
            currentError = Exception("Camera permission not granted")
            return
        }

        isCapturing = true
        currentError = null

        // For now, simulate camera capture with a placeholder
        // In a real implementation, this would use CameraX or system camera
        try {
            // Simulate capture delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isCapturing = false
                // Create a placeholder URI - in real implementation, this would be actual photo
                val placeholderUri = Uri.parse("content://com.digitalwardrobe.placeholder/photo")
                onPhotoCaptured(placeholderUri)
            }, 1000)
        } catch (e: Exception) {
            isCapturing = false
            currentError = e
        }
    }
}

/**
 * Camera capture manager for composables.
 */
@Composable
fun rememberCameraCapture(
    onPhotoCaptured: (Uri) -> Unit
): CameraCaptureState {
    val context = LocalContext.current
    val cameraCaptureState = remember { CameraCaptureState() }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraCaptureState.hasPermission = isGranted
    }

    // Initialize camera permission check
    LaunchedEffect(Unit) {
        cameraCaptureState.hasPermission = CameraUtils.hasCameraPermission(context)
    }

    // Request permission if needed
    fun requestPermission() {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    return cameraCaptureState.apply {
        this.requestPermission = ::requestPermission
    }
}

/**
 * Simple camera capture using system camera app.
 * This is a simplified implementation for demonstration.
 */
fun Context.launchSystemCamera(): Uri? {
    // For now, return null - in a real implementation, this would launch the camera
    // and return the captured photo URI
    return null
}

/**
 * Gallery picker state holder.
 */
class GalleryPickerState {
    var isPicking by mutableStateOf(false)
        internal set

    var currentError by mutableStateOf<Exception?>(null)
        internal set

    lateinit var pickImage: () -> Unit
        internal set
}

/**
 * Gallery picker manager for composables.
 * Uses Photo Picker API for Android 13+ and falls back to ACTION_GET_CONTENT for older versions.
 */
@Composable
fun rememberGalleryPicker(
    onImageSelected: (Uri) -> Unit
): GalleryPickerState {
    val context = LocalContext.current
    val galleryPickerState = remember { GalleryPickerState() }

    // Modern Photo Picker (Android 13+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        galleryPickerState.isPicking = false
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    // Fallback for older Android versions
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        galleryPickerState.isPicking = false
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    // Pick image function
    fun pickImage() {
        galleryPickerState.isPicking = true
        galleryPickerState.currentError = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use modern Photo Picker
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                // Use legacy picker
                legacyPickerLauncher.launch("image/*")
            }
        } catch (e: Exception) {
            galleryPickerState.isPicking = false
            galleryPickerState.currentError = e
        }
    }

    return galleryPickerState.apply {
        this.pickImage = ::pickImage
    }
}
