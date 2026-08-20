// NoteVault-iOS/Persistence/CoreDataEntities.swift

import Foundation
import CoreData

@objc(NoteEntity)
public class NoteEntity: NSManagedObject {
    @NSManaged public var id: UUID?
    @NSManaged public var title: String?
    @NSManaged public var content: String?
    @NSManaged public var folderId: UUID?
    @NSManaged public var createdAt: Date?
    @NSManaged public var updatedAt: Date?
    @NSManaged public var isPinned: Bool
    @NSManaged public var isArchived: Bool
    @NSManaged public var isLocked: Bool
    @NSManaged public var colorHex: String?
    @NSManaged public var tagIdsData: Data?
}

@objc(FolderEntity)
public class FolderEntity: NSManagedObject {
    @NSManaged public var id: UUID?
    @NSManaged public var name: String?
    @NSManaged public var iconName: String?
    @NSManaged public var colorHex: String?
    @NSManaged public var parentFolderId: UUID?
    @NSManaged public var createdAt: Date?
}

@objc(TagEntity)
public class TagEntity: NSManagedObject {
    @NSManaged public var id: UUID?
    @NSManaged public var name: String?
    @NSManaged public var colorHex: String?
}

@objc(TaskEntity)
public class TaskEntity: NSManagedObject {
    @NSManaged public var id: UUID?
    @NSManaged public var noteId: UUID?
    @NSManaged public var title: String?
    @NSManaged public var isCompleted: Bool
    @NSManaged public var dueDate: Date?
    @NSManaged public var reminderDate: Date?
    @NSManaged public var priority: Int16
    @NSManaged public var createdAt: Date?
}

@objc(DiagramEntity)
public class DiagramEntity: NSManagedObject {
    @NSManaged public var id: UUID?
    @NSManaged public var title: String?
    @NSManaged public var templateType: String?
    @NSManaged public var nodesData: Data?
    @NSManaged public var edgesData: Data?
    @NSManaged public var viewportScale: Double
    @NSManaged public var viewportOffsetX: Double
    @NSManaged public var viewportOffsetY: Double
    @NSManaged public var associatedNoteId: UUID?
    @NSManaged public var updatedAt: Date?
}
