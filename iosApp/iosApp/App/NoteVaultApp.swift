// NoteVault-iOS/App/NoteVaultApp.swift

import SwiftUI

@main
struct NoteVaultApp: App {
    @StateObject private var securityViewModel = SecurityViewModel()
    @StateObject private var appState = AppState.shared
    @Environment(\.scenePhase) private var scenePhase
    
    private let persistenceController = PersistenceController.shared
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                MainNavigationView()
                    .environment(\.managedObjectContext, persistenceController.container.viewContext)
                    .environmentObject(appState)
                
                if securityViewModel.isAppLocked {
                    AppLockView(securityViewModel: securityViewModel)
                        .transition(.opacity)
                }
            }
            .animation(.easeInOut(duration: 0.25), value: securityViewModel.isAppLocked)
            .onChange(of: scenePhase) { _, newPhase in
                if newPhase == .background || newPhase == .inactive {
                    securityViewModel.lockApp()
                }
            }
        }
    }
}
