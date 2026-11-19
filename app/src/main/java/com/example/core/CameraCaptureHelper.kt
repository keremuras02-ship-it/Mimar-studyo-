package com.example.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

private fun createTempRoomPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captured_rooms").apply { mkdirs() }
    val imageFile = File(dir, "room_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

/**
 * Reusable Compose helper handling runtime CAMERA permission request and full-resolution
 * photo capture via [ActivityResultContracts.TakePicture] + [FileProvider].
 */
@Composable
fun rememberRoomCameraCapture(
    maxDimension: Int = 1280,
    onPhotoCaptured: (Bitmap) -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            scope.launch {
                val bitmap = BitmapUtils.decodeScaledBitmap(context, uri, maxDimension)
                if (bitmap != null) {
                    onPhotoCaptured(bitmap)
                }
            }
        }
    }

    fun launchCameraCapture() {
        val uri = createTempRoomPhotoUri(context)
        pendingCaptureUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            onPermissionDenied()
        }
    }

    return remember(context) {
        {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                launchCameraCapture()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
