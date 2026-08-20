// NoteVault-iOS/Models/Domain/Folder.swift

import Foundation

public struct Folder: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var name: String
    public var iconName: String
    public var colorHex: String?
    public var parentFolderId: UUID?
    public var createdAt: Date
    
    public init(
        id: UUID = UUID(),
        name: String,
        iconName: String = "folder.fill",
        colorHex: String? = nil,
        parentFolderId: UUID? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.iconName = iconName
        self.colorHex = colorHex
        self.parentFolderId = parentFolderId
        self.createdAt = createdAt
    }
}
