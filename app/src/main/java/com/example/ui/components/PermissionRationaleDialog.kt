package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.PermissionExplanation
import com.example.platform.PermissionType

@Composable
fun PermissionRationaleDialog(
    explanation: PermissionExplanation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon = when (explanation.permissionType) {
        PermissionType.NOTIFICATIONS -> Icons.Default.NotificationsActive
        PermissionType.EXACT_ALARMS -> Icons.Default.Alarm
        PermissionType.CAMERA -> Icons.Default.PhotoCamera
        PermissionType.PHOTO_LIBRARY -> Icons.Default.PhotoLibrary
        PermissionType.BIOMETRICS -> Icons.Default.Fingerprint
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                explanation.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    explanation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "All permissions are handled 100% locally on your device without any external network calls.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Allow Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
