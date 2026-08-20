// NoteVault-iOS/Core/Security/BiometricAuthManager.swift

import Foundation
import LocalAuthentication

public enum BiometricType: Sendable {
    case none
    case touchID
    case faceID
    case opticID
    
    public var title: String {
        switch self {
        case .none: return "Passcode"
        case .touchID: return "Touch ID"
        case .faceID: return "Face ID"
        case .opticID: return "Optic ID"
        }
    }
    
    public var iconName: String {
        switch self {
        case .none: return "lock.fill"
        case .touchID: return "touchid"
        case .faceID: return "faceid"
        case .opticID: return "opticid"
        }
    }
}

public final class BiometricAuthManager: @unchecked Sendable {
    public static let shared = BiometricAuthManager()
    
    private init() {}
    
    public var availableBiometryType: BiometricType {
        let context = LAContext()
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            switch context.biometryType {
            case .faceID: return .faceID
            case .touchID: return .touchID
            case .opticID: return .opticID
            default: return .none
            }
        }
        return .none
    }
    
    public func authenticate(reason: String = "Unlock NoteVault to access encrypted notes") async -> (success: Bool, error: Error?) {
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"
        
        var error: NSError?
        // Fallback to device passcode if biometrics fail or are unavailable
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            do {
                let success = try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
                return (success, nil)
            } catch {
                // Fallback to device passcode
                do {
                    let fallbackSuccess = try await context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
                    return (fallbackSuccess, nil)
                } catch let fallbackError {
                    return (false, fallbackError)
                }
            }
        } else if context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
            do {
                let success = try await context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
                return (success, nil)
            } catch let authError {
                return (false, authError)
            }
        } else {
            return (false, error)
        }
    }
}
