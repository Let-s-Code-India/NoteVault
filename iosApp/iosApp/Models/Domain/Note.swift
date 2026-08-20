// NoteVault-iOS/Models/Domain/Note.swift

import Foundation

public struct Note: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var title: String
    public var content: String
    public var folderId: UUID?
    public var tagIds: Set<UUID>
    public var createdAt: Date
    public var updatedAt: Date
    public var isPinned: Bool
    public var isArchived: Bool
    public var isLocked: Bool
    public var colorHex: String?
    
    public init(
        id: UUID = UUID(),
        title: String = "",
        content: String = "",
        folderId: UUID? = nil,
        tagIds: Set<UUID> = [],
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
        isPinned: Bool = false,
        isArchived: Bool = false,
        isLocked: Bool = false,
        colorHex: String? = nil
    ) {
        self.id = id
        self.title = title
        self.content = content
        self.folderId = folderId
        self.tagIds = tagIds
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.isPinned = isPinned
        self.isArchived = isArchived
        self.isLocked = isLocked
        self.colorHex = colorHex
    }
    
    /// Plain preview snippet computed from content markdown / text
    public var previewSnippet: String {
        let clean = content
            .replacingOccurrences(of: "#", with: "")
            .replacingOccurrences(of: "*", with: "")
            .replacingOccurrences(of: "`", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return clean.isEmpty ? "No additional text" : clean
    }
}
