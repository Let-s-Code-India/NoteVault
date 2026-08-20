// NoteVault-iOS/Models/Domain/SecuritySettings.swift

import Foundation

public struct SecuritySettings: Codable, Sendable {
    public var isBiometricEnabled: Bool
    public var isAppLockEnabled: Bool
    public var autoLockIntervalSeconds: Int // 0: Immediately, 60: 1 min, 300: 5 min
    public var isEncryptedStorageEnabled: Bool
    
    public init(
        isBiometricEnabled: Bool = false,
        isAppLockEnabled: Bool = false,
        autoLockIntervalSeconds: Int = 0,
        isEncryptedStorageEnabled: Bool = true
    ) {
        self.isBiometricEnabled = isBiometricEnabled
        self.isAppLockEnabled = isAppLockEnabled
        self.autoLockIntervalSeconds = autoLockIntervalSeconds
        self.isEncryptedStorageEnabled = isEncryptedStorageEnabled
    }
}
