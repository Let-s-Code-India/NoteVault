// NoteVault-iOS/App/AppState.swift

import Foundation
import Combine

@MainActor
public final class AppState: ObservableObject {
    public static let shared = AppState()
    
    @Published public var activeNoteId: UUID?
    @Published public var isPrivacyShieldActive: Bool = false
    
    private init() {}
}
