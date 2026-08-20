// NoteVault-iOS/Core/Security/EncryptionManager.swift

import Foundation
import CryptoKit

public enum EncryptionError: Error {
    case keyGenerationFailed
    case encryptionFailed
    case decryptionFailed
    case invalidPayload
}

public final class EncryptionManager: @unchecked Sendable {
    public static let shared = EncryptionManager()
    private let databaseKeyIdentifier = "notevault_master_aes_key"
    private var symmetricKey: SymmetricKey?
    private let lock = NSLock()
    
    private init() {
        self.symmetricKey = try? loadOrGenerateKey()
    }
    
    private func loadOrGenerateKey() throws -> SymmetricKey {
        lock.lock()
        defer { lock.unlock() }
        
        if let existingData = try? KeychainService.shared.retrieve(key: databaseKeyIdentifier) {
            return SymmetricKey(data: existingData)
        }
        
        let newKey = SymmetricKey(size: .bits256)
        let keyData = newKey.withUnsafeBytes { Data($0) }
        try KeychainService.shared.save(key: databaseKeyIdentifier, data: keyData)
        return newKey
    }
    
    public func encrypt(plainText: String) throws -> String {
        guard let key = symmetricKey ?? (try? loadOrGenerateKey()) else {
            throw EncryptionError.keyGenerationFailed
        }
        guard let data = plainText.data(using: .utf8) else {
            throw EncryptionError.invalidPayload
        }
        
        do {
            let sealedBox = try AES.GCM.seal(data, using: key)
            guard let combined = sealedBox.combined else {
                throw EncryptionError.encryptionFailed
            }
            return combined.base64EncodedString()
        } catch {
            throw EncryptionError.encryptionFailed
        }
    }
    
    public func decrypt(cipherBase64: String) throws -> String {
        guard let key = symmetricKey ?? (try? loadOrGenerateKey()) else {
            throw EncryptionError.keyGenerationFailed
        }
        guard let combinedData = Data(base64Encoded: cipherBase64) else {
            // Fallback: If text was unencrypted (legacy), return raw text
            return cipherBase64
        }
        
        do {
            let sealedBox = try AES.GCM.SealedBox(combined: combinedData)
            let decryptedData = try AES.GCM.open(sealedBox, using: key)
            guard let plainText = String(data: decryptedData, encoding: .utf8) else {
                throw EncryptionError.decryptionFailed
            }
            return plainText
        } catch {
            // If AES-GCM fails, it might be raw unencrypted plaintext from import
            return cipherBase64
        }
    }
}
