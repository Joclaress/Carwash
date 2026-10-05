package com.example.carwash.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.carwash.utils.ImageCompressor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraCaptureBox(
    title: String,
    imageUri: Uri?,
    onImageCaptured: (Uri) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Preserve pendingUri across Activity recreation when Camera app opens
    var pendingUriStr by rememberSaveable { mutableStateOf<String?>(null) }
    var compressing by remember { mutableStateOf(false) }
    var isLaunchingCamera by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        isLaunchingCamera = false
        val capturedUriStr = pendingUriStr
        val capturedUri = capturedUriStr?.let { Uri.parse(it) }

        if (success && capturedUri != null) {
            coroutineScope.launch {
                try {
                    compressing = true
                    // Give a brief delay if file write is finalizing
                    delay(100)
                    val compressed = ImageCompressor.compress(context, capturedUri)
                    onImageCaptured(compressed)
                } catch (e: Exception) {
                    Log.e("CameraCaptureBox", "Error compressing image: ${e.message}", e)
                    // Fallback to captured Uri directly
                    onImageCaptured(capturedUri)
                } finally {
                    compressing = false
                    pendingUriStr = null
                }
            }
        } else {
            Log.w("CameraCaptureBox", "Camera capture cancelled or failed. success=$success, uri=$capturedUriStr")
            isLaunchingCamera = false
            compressing = false
            pendingUriStr = null
        }
    }

    fun openCamera() {
        if (isLaunchingCamera || compressing) return
        try {
            isLaunchingCamera = true
            val uri = createCameraUri(context)
            pendingUriStr = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            isLaunchingCamera = false
            Log.e("CameraCaptureBox", "Failed to launch camera: ${e.message}", e)
            Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clickable(enabled = !compressing && !isLaunchingCamera) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        openCamera()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    compressing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Processing image...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    imageUri == null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tap to Take Photo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Camera photo capture",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    else -> {
                        Box {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Captured Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = onRemove,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            FilledTonalButton(
                                onClick = { openCamera() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                enabled = !compressing && !isLaunchingCamera
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retake Photo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val folder = File(context.cacheDir, "camera")
    if (!folder.exists()) {
        folder.mkdirs()
    }
    // Unique timestamped file to avoid cache conflicts or overwriting
    val file = File(folder, "capture_${System.currentTimeMillis()}.jpg")
    if (file.exists()) {
        file.delete()
    }
    file.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
