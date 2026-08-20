// NoteVault-iOS/ViewModels/SecurityViewModel.swift

import Foundation
import Combine
import SwiftUI

@MainActor
public final class SecurityViewModel: ObservableObject {
    @Published public var isAppLocked: Bool = false
    @Published public var settings: SecuritySettings = SecuritySettings()
    @Published public var biometricType: BiometricType = .none
    @Published public var authErrorMessage: String?
    
    private let authManager: BiometricAuthManager
    private let userDefaultsKey = "notevault_security_settings"
    
    public init(authManager: BiometricAuthManager = .shared) {
        self.authManager = authManager
        self.biometricType = authManager.availableBiometryType
        loadSettings()
        
        if settings.isAppLockEnabled {
            self.isAppLocked = true
        }
    }
    
    public func loadSettings() {
        if let data = UserDefaults.standard.data(forKey: userDefaultsKey),
           let decoded = try? JSONDecoder().decode(SecuritySettings.self, from: data) {
            self.settings = decoded
        }
    }
    
    public func saveSettings() {
        if let encoded = try? JSONEncoder().encode(settings) {
            UserDefaults.standard.set(encoded, forKey: userDefaultsKey)
        }
    }
    
    public func authenticateToUnlock() async {
        authErrorMessage = nil
        let result = await authManager.authenticate()
        if result.success {
            self.isAppLocked = false
        } else {
            self.authErrorMessage = result.error?.localizedDescription ?? "Authentication failed."
        }
    }
    
    public func lockApp() {
        if settings.isAppLockEnabled {
            self.isAppLocked = true
        }
    }
    
    public func toggleAppLock(enabled: Bool) async {
        if enabled {
            // Require initial authentication before enabling lock
            let result = await authManager.authenticate(reason: "Enable biometric lock for NoteVault")
            if result.success {
                settings.isAppLockEnabled = true
                settings.isBiometricEnabled = true
                saveSettings()
            }
        } else {
            let result = await authManager.authenticate(reason: "Authenticate to disable app lock")
            if result.success {
                settings.isAppLockEnabled = false
                settings.isBiometricEnabled = false
                self.isAppLocked = false
                saveSettings()
            }
        }
    }
}
