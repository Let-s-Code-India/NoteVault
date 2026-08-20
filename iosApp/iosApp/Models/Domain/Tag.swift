// NoteVault-iOS/Models/Domain/Tag.swift

import Foundation

public struct Tag: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var name: String
    public var colorHex: String
    
    public init(
        id: UUID = UUID(),
        name: String,
        colorHex: String = "#0A84FF"
    ) {
        self.id = id
        self.name = name.hasPrefix("#") ? name : "#\(name)"
        self.colorHex = colorHex
    }
}
