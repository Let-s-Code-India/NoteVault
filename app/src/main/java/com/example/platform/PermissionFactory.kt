package com.example.platform

fun createPermissionManager(): PermissionManager {
    val context = getAndroidPlatformContext()
    return AndroidPermissionManager(context)
}
