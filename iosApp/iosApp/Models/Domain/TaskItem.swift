// NoteVault-iOS/Models/Domain/TaskItem.swift

import Foundation

public enum TaskPriority: Int16, CaseIterable, Codable, Sendable {
    case low = 0
    case medium = 1
    case high = 2
    
    public var title: String {
        switch self {
        case .low: return "Low"
        case .medium: return "Medium"
        case .high: return "High"
        }
    }
    
    public var colorHex: String {
        switch self {
        case .low: return "#30D158"
        case .medium: return "#FF9F0A"
        case .high: return "#FF453A"
        }
    }
}

public struct TaskItem: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var noteId: UUID?
    public var title: String
    public var isCompleted: Bool
    public var dueDate: Date?
    public var reminderDate: Date?
    public var priority: TaskPriority
    public var createdAt: Date
    
    public init(
        id: UUID = UUID(),
        noteId: UUID? = nil,
        title: String,
        isCompleted: Bool = false,
        dueDate: Date? = nil,
        reminderDate: Date? = nil,
        priority: TaskPriority = .medium,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.noteId = noteId
        self.title = title
        self.isCompleted = isCompleted
        self.dueDate = dueDate
        self.reminderDate = reminderDate
        self.priority = priority
        self.createdAt = createdAt
    }
    
    public var isOverdue: Bool {
        guard let due = dueDate, !isCompleted else { return false }
        return due < Date()
    }
}
