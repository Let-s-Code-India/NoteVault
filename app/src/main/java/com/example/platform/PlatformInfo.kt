package com.example.platform

enum class PlatformType {
    ANDROID,
    IOS,
    DESKTOP
}

enum class FormFactor {
    PHONE,
    TABLET,
    DESKTOP
}

val currentPlatform: PlatformType = PlatformType.ANDROID
val platformDisplayName: String = "Android"
