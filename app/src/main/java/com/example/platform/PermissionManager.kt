package com.example.platform

enum class PermissionType {
    NOTIFICATIONS,
    EXACT_ALARMS,
    CAMERA,
    PHOTO_LIBRARY,
    BIOMETRICS
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_DETERMINED,
    NOT_APPLICABLE
}

data class PermissionExplanation(
    val title: String,
    val description: String,
    val permissionType: PermissionType
)

interface PermissionManager {
    fun getPermissionStatus(permission: PermissionType): PermissionStatus
    fun isPermissionGranted(permission: PermissionType): Boolean
    fun requestPermission(
        permission: PermissionType,
        onResult: (PermissionStatus) -> Unit
    )
    fun getPermissionExplanation(permission: PermissionType): PermissionExplanation
}
