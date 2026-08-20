// NoteVault-iOS/Views/Security/AppLockView.swift

import SwiftUI

public struct AppLockView: View {
    @ObservedObject var securityViewModel: SecurityViewModel
    
    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 24) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 72))
                    .foregroundColor(.accentColor)
                
                VStack(spacing: 8) {
                    Text("NoteVault Locked")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    
                    Text("Unlock with Face ID, Touch ID, or Passcode")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
                
                Button {
                    Task {
                        await securityViewModel.authenticateToUnlock()
                    }
                } label: {
                    Label("Unlock Vault", systemImage: securityViewModel.biometricType.iconName)
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 14)
                        .background(Color.accentColor)
                        .clipShape(Capsule())
                }
                .padding(.top, 16)
                
                if let error = securityViewModel.authErrorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
            }
        }
        .task {
            await securityViewModel.authenticateToUnlock()
        }
    }
}
