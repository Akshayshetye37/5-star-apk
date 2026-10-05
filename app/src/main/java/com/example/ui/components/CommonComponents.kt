package com.example.ui.components

import android.net.Uri
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.util.IdOcrRecognizer
import com.example.util.AppPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import java.io.File

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "AVAILABLE" -> Triple(SuccessGreenContainer, Color(0xFF065F46), "Available")
        "OCCUPIED" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Occupied")
        "RESERVED" -> Triple(WarningAmberContainer, Color(0xFF92400E), "Reserved")
        "MAINTENANCE" -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), "Maintenance")
        "ACTIVE" -> Triple(SuccessGreenContainer, Color(0xFF065F46), "Active")
        "CHECKED_OUT", "CHECKED-IN", "COMPLETED" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), status.replace("_", " "))
        "PAID" -> Triple(SuccessGreenContainer, Color(0xFF065F46), "Paid")
        "PARTIALLY_PAID" -> Triple(WarningAmberContainer, Color(0xFF92400E), "Partially Paid")
        "PENDING" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Pending")
        "CANCELLED" -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Cancelled")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("status_chip_$status")
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = HotelNavy,
    bgColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message, color = Slate700) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IdPhotoPicker(
    photoUri: String,
    onPhotoSelected: (String) -> Unit,
    onOcrResult: ((com.example.util.IdOcrResult) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempCameraUri: Uri? = null
    var pendingOcr by remember { mutableStateOf<com.example.util.IdOcrResult?>(null) }

    fun handleSavedPhoto(path: String) {
        onPhotoSelected(path)
        if (onOcrResult != null && AppPreferences.ocrEnabled(context)) {
            scope.launch {
                val result = IdOcrRecognizer.recognize(context, path)
                if (result.name.isNotBlank() || result.idNumber.isNotBlank() || result.idType.isNotBlank()) {
                    pendingOcr = result
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy to internal storage
            try {
                val input = context.contentResolver.openInputStream(it)
                val destFile = File(context.filesDir, "id_${System.currentTimeMillis()}.jpg")
                val out = java.io.FileOutputStream(destFile)
                input?.copyTo(out)
                input?.close()
                out.close()
                handleSavedPhoto(destFile.absolutePath)
            } catch (e: Exception) {
                handleSavedPhoto(it.toString())
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            try {
                val source = context.contentResolver.openInputStream(tempCameraUri!!)
                val destFile = File(context.filesDir, "id_${System.currentTimeMillis()}.jpg")
                source?.use { input -> FileOutputStream(destFile).use { output -> input.copyTo(output) } }
                handleSavedPhoto(destFile.absolutePath)
            } catch (_: Exception) {
                handleSavedPhoto(tempCameraUri.toString())
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Customer ID Document / Photo",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Slate700
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (photoUri.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
            ) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Customer ID Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { onPhotoSelected("") },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Photo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replace Photo")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val tempFile = File(context.cacheDir, "camera_id_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            galleryLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("take_id_photo_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("upload_id_photo_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select File")
                }
            }
        }
        pendingOcr?.let { result ->
            AlertDialog(
                onDismissRequest = { pendingOcr = null },
                title = { Text("ID details detected") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Document: ${result.idType.ifBlank { "Not certain" }}")
                        Text("Name: ${result.name.ifBlank { "Not detected" }}")
                        Text("ID number: ${result.idNumber.ifBlank { "Not detected" }}")
                        Text(result.confidenceHint, color = Slate500, fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onOcrResult?.invoke(result)
                        pendingOcr = null
                    }) { Text("Use detected details") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingOcr = null }) { Text("Review manually") }
                }
            )
        }

    }
}
