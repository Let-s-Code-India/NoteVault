// NoteVault-iOS/Views/Security/SettingsView.swift

import SwiftUI

public struct SettingsView: View {
    @StateObject private var securityViewModel = SecurityViewModel()
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            Form {
                Section("Security & Encryption") {
                    Toggle("Biometric App Lock", isOn: Binding(
                        get: { securityViewModel.settings.isAppLockEnabled },
                        set: { newValue in
                            Task {
                                await securityViewModel.toggleAppLock(enabled: newValue)
                            }
                        }
                    ))
                    
                    HStack {
                        Text("Local Encryption")
                        Spacer()
                        Text("AES-256 GCM")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                
                Section("About NoteVault") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0 (iOS)")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Storage")
                        Spacer()
                        Text("Offline / On-Device Only")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Network Access")
                        Spacer()
                        Text("None")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}
