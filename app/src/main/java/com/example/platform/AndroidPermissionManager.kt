package com.example.platform

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat

class AndroidPermissionManager(private val context: Context) : PermissionManager {

    override fun getPermissionStatus(permission: PermissionType): PermissionStatus {
        return when (permission) {
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    if (status == PackageManager.PERMISSION_GRANTED) PermissionStatus.GRANTED else PermissionStatus.DENIED
                } else {
                    PermissionStatus.GRANTED
                }
            }
            PermissionType.EXACT_ALARMS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (alarmManager?.canScheduleExactAlarms() == true) {
                        PermissionStatus.GRANTED
                    } else {
                        PermissionStatus.DENIED
                    }
                } else {
                    PermissionStatus.GRANTED
                }
            }
            PermissionType.CAMERA -> {
                val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                if (status == PackageManager.PERMISSION_GRANTED) PermissionStatus.GRANTED else PermissionStatus.DENIED
            }
            PermissionType.PHOTO_LIBRARY -> {
                // Android utilizes the system content picker (Storage Access Framework / Photo Picker)
                // which does not require runtime storage permission from the application.
                PermissionStatus.GRANTED
            }
            PermissionType.BIOMETRICS -> {
                val biometricManager = BiometricManager.from(context)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> PermissionStatus.GRANTED
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> PermissionStatus.NOT_APPLICABLE
                    else -> PermissionStatus.DENIED
                }
            }
        }
    }

    override fun isPermissionGranted(permission: PermissionType): Boolean {
        return getPermissionStatus(permission) == PermissionStatus.GRANTED
    }

    override fun requestPermission(permission: PermissionType, onResult: (PermissionStatus) -> Unit) {
        onResult(getPermissionStatus(permission))
    }

    override fun getPermissionExplanation(permission: PermissionType): PermissionExplanation {
        return when (permission) {
            PermissionType.NOTIFICATIONS -> PermissionExplanation(
                title = "Notifications Permission",
                description = "NoteVault requires notification permission to alert you when your scheduled note and task reminders are due.",
                permissionType = permission
            )
            PermissionType.EXACT_ALARMS -> PermissionExplanation(
                title = "Exact Alarms Permission",
                description = "Exact alarm scheduling ensures your reminders trigger precisely on time, even when the device is in battery saver or doze mode.",
                permissionType = permission
            )
            PermissionType.CAMERA -> PermissionExplanation(
                title = "Camera Permission",
                description = "Camera access is needed to capture photos directly into your notes or design canvas compositions.",
                permissionType = permission
            )
            PermissionType.PHOTO_LIBRARY -> PermissionExplanation(
                title = "Photo Library Access",
                description = "System file and photo picker allows you to select images to insert into your markdown notes and drawings.",
                permissionType = permission
            )
            PermissionType.BIOMETRICS -> PermissionExplanation(
                title = "Biometric Authentication",
                description = "Fingerprint or face recognition enables instant, secure unlocking of your encrypted vault.",
                permissionType = permission
            )
        }
    }
}
