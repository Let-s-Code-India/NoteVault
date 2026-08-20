// NoteVault-iOS/Persistence/NoteVaultDAO.swift

import Foundation
import CoreData

public protocol NoteVaultDAOSwift: Sendable {
    // Notes
    func fetchNotes() async throws -> [Note]
    func saveNote(_ note: Note) async throws
    func deleteNote(id: UUID) async throws
    
    // Folders
    func fetchFolders() async throws -> [Folder]
    func saveFolder(_ folder: Folder) async throws
    func deleteFolder(id: UUID) async throws
    
    // Tags
    func fetchTags() async throws -> [Tag]
    func saveTag(_ tag: Tag) async throws
    func deleteTag(id: UUID) async throws
    
    // Tasks
    func fetchTasks() async throws -> [TaskItem]
    func saveTask(_ task: TaskItem) async throws
    func deleteTask(id: UUID) async throws
    
    // Diagrams / Logic Boards
    func fetchDiagrams() async throws -> [LogicBoard]
    func saveDiagram(_ diagram: LogicBoard) async throws
    func deleteDiagram(id: UUID) async throws
}

public final class NoteVaultDAO: NoteVaultDAOSwift {
    private let persistenceController: PersistenceController
    private let jsonEncoder = JSONEncoder()
    private let jsonDecoder = JSONDecoder()
    
    public init(persistenceController: PersistenceController = .shared) {
        self.persistenceController = persistenceController
    }
    
    // MARK: - Notes DAO
    
    public func fetchNotes() async throws -> [Note] {
        let context = persistenceController.container.newBackgroundContext()
        return try await context.perform {
            let request: NSFetchRequest<NoteEntity> = NSFetchRequest(entityName: "NoteEntity")
            request.sortDescriptors = [
                NSSortDescriptor(key: "isPinned", ascending: false),
                NSSortDescriptor(key: "updatedAt", ascending: false)
            ]
            let entities = try context.fetch(request)
            return entities.compactMap { entity -> Note? in
                guard let id = entity.id,
                      let title = entity.title,
                      let cipherContent = entity.content,
                      let createdAt = entity.createdAt,
                      let updatedAt = entity.updatedAt else {
                    return nil
                }
                
                let plainContent = (try? EncryptionManager.shared.decrypt(cipherBase64: cipherContent)) ?? cipherContent
                var tagIds = Set<UUID>()
                if let data = entity.tagIdsData,
                   let decoded = try? self.jsonDecoder.decode(Set<UUID>.self, from: data) {
                    tagIds = decoded
                }
                
                return Note(
                    id: id,
                    title: title,
                    content: plainContent,
                    folderId: entity.folderId,
                    tagIds: tagIds,
                    createdAt: createdAt,
                    updatedAt: updatedAt,
                    isPinned: entity.isPinned,
                    isArchived: entity.isArchived,
                    isLocked: entity.isLocked,
                    colorHex: entity.colorHex
                )
            }
        }
    }
    
    public func saveNote(_ note: Note) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<NoteEntity> = NSFetchRequest(entityName: "NoteEntity")
            request.predicate = NSPredicate(format: "id == %@", note.id as CVarArg)
            let existing = try context.fetch(request).first
            let entity = existing ?? NoteEntity(context: context)
            
            entity.id = note.id
            entity.title = note.title
            entity.content = (try? EncryptionManager.shared.encrypt(plainText: note.content)) ?? note.content
            entity.folderId = note.folderId
            entity.createdAt = note.createdAt
            entity.updatedAt = note.updatedAt
            entity.isPinned = note.isPinned
            entity.isArchived = note.isArchived
            entity.isLocked = note.isLocked
            entity.colorHex = note.colorHex
            entity.tagIdsData = try? self.jsonEncoder.encode(note.tagIds)
            
            try context.save()
        }
    }
    
    public func deleteNote(id: UUID) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<NoteEntity> = NSFetchRequest(entityName: "NoteEntity")
            request.predicate = NSPredicate(format: "id == %@", id as CVarArg)
            if let entity = try context.fetch(request).first {
                context.delete(entity)
                try context.save()
            }
        }
    }
    
    // MARK: - Folders DAO
    
    public func fetchFolders() async throws -> [Folder] {
        let context = persistenceController.container.newBackgroundContext()
        return try await context.perform {
            let request: NSFetchRequest<FolderEntity> = NSFetchRequest(entityName: "FolderEntity")
            request.sortDescriptors = [NSSortDescriptor(key: "name", ascending: true)]
            let entities = try context.fetch(request)
            return entities.compactMap { entity in
                guard let id = entity.id,
                      let name = entity.name,
                      let icon = entity.iconName,
                      let createdAt = entity.createdAt else { return nil }
                return Folder(
                    id: id,
                    name: name,
                    iconName: icon,
                    colorHex: entity.colorHex,
                    parentFolderId: entity.parentFolderId,
                    createdAt: createdAt
                )
            }
        }
    }
    
    public func saveFolder(_ folder: Folder) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<FolderEntity> = NSFetchRequest(entityName: "FolderEntity")
            request.predicate = NSPredicate(format: "id == %@", folder.id as CVarArg)
            let entity = try context.fetch(request).first ?? FolderEntity(context: context)
            
            entity.id = folder.id
            entity.name = folder.name
            entity.iconName = folder.iconName
            entity.colorHex = folder.colorHex
            entity.parentFolderId = folder.parentFolderId
            entity.createdAt = folder.createdAt
            
            try context.save()
        }
    }
    
    public func deleteFolder(id: UUID) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<FolderEntity> = NSFetchRequest(entityName: "FolderEntity")
            request.predicate = NSPredicate(format: "id == %@", id as CVarArg)
            if let entity = try context.fetch(request).first {
                context.delete(entity)
                try context.save()
            }
        }
    }
    
    // MARK: - Tags DAO
    
    public func fetchTags() async throws -> [Tag] {
        let context = persistenceController.container.newBackgroundContext()
        return try await context.perform {
            let request: NSFetchRequest<TagEntity> = NSFetchRequest(entityName: "TagEntity")
            request.sortDescriptors = [NSSortDescriptor(key: "name", ascending: true)]
            let entities = try context.fetch(request)
            return entities.compactMap { entity in
                guard let id = entity.id, let name = entity.name, let color = entity.colorHex else { return nil }
                return Tag(id: id, name: name, colorHex: color)
            }
        }
    }
    
    public func saveTag(_ tag: Tag) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<TagEntity> = NSFetchRequest(entityName: "TagEntity")
            request.predicate = NSPredicate(format: "id == %@", tag.id as CVarArg)
            let entity = try context.fetch(request).first ?? TagEntity(context: context)
            
            entity.id = tag.id
            entity.name = tag.name
            entity.colorHex = tag.colorHex
            
            try context.save()
        }
    }
    
    public func deleteTag(id: UUID) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<TagEntity> = NSFetchRequest(entityName: "TagEntity")
            request.predicate = NSPredicate(format: "id == %@", id as CVarArg)
            if let entity = try context.fetch(request).first {
                context.delete(entity)
                try context.save()
            }
        }
    }
    
    // MARK: - Tasks DAO
    
    public func fetchTasks() async throws -> [TaskItem] {
        let context = persistenceController.container.newBackgroundContext()
        return try await context.perform {
            let request: NSFetchRequest<TaskEntity> = NSFetchRequest(entityName: "TaskEntity")
            request.sortDescriptors = [
                NSSortDescriptor(key: "isCompleted", ascending: true),
                NSSortDescriptor(key: "priority", ascending: false),
                NSSortDescriptor(key: "dueDate", ascending: true)
            ]
            let entities = try context.fetch(request)
            return entities.compactMap { entity in
                guard let id = entity.id,
                      let title = entity.title,
                      let createdAt = entity.createdAt else { return nil }
                return TaskItem(
                    id: id,
                    noteId: entity.noteId,
                    title: title,
                    isCompleted: entity.isCompleted,
                    dueDate: entity.dueDate,
                    reminderDate: entity.reminderDate,
                    priority: TaskPriority(rawValue: entity.priority) ?? .medium,
                    createdAt: createdAt
                )
            }
        }
    }
    
    public func saveTask(_ task: TaskItem) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<TaskEntity> = NSFetchRequest(entityName: "TaskEntity")
            request.predicate = NSPredicate(format: "id == %@", task.id as CVarArg)
            let entity = try context.fetch(request).first ?? TaskEntity(context: context)
            
            entity.id = task.id
            entity.noteId = task.noteId
            entity.title = task.title
            entity.isCompleted = task.isCompleted
            entity.dueDate = task.dueDate
            entity.reminderDate = task.reminderDate
            entity.priority = task.priority.rawValue
            entity.createdAt = task.createdAt
            
            try context.save()
        }
    }
    
    public func deleteTask(id: UUID) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<TaskEntity> = NSFetchRequest(entityName: "TaskEntity")
            request.predicate = NSPredicate(format: "id == %@", id as CVarArg)
            if let entity = try context.fetch(request).first {
                context.delete(entity)
                try context.save()
            }
        }
    }
    
    // MARK: - Logic Board / Diagram DAO
    
    public func fetchDiagrams() async throws -> [LogicBoard] {
        let context = persistenceController.container.newBackgroundContext()
        return try await context.perform {
            let request: NSFetchRequest<DiagramEntity> = NSFetchRequest(entityName: "DiagramEntity")
            request.sortDescriptors = [NSSortDescriptor(key: "updatedAt", ascending: false)]
            let entities = try context.fetch(request)
            return entities.compactMap { entity in
                guard let id = entity.id,
                      let title = entity.title,
                      let rawType = entity.templateType,
                      let template = DiagramTemplateType(rawValue: rawType),
                      let nodesData = entity.nodesData,
                      let edgesData = entity.edgesData,
                      let nodes = try? self.jsonDecoder.decode([BoardNode].self, from: nodesData),
                      let edges = try? self.jsonDecoder.decode([BoardEdge].self, from: edgesData),
                      let updatedAt = entity.updatedAt else {
                    return nil
                }
                return LogicBoard(
                    id: id,
                    title: title,
                    templateType: template,
                    nodes: nodes,
                    edges: edges,
                    viewportScale: CGFloat(entity.viewportScale),
                    viewportOffsetX: CGFloat(entity.viewportOffsetX),
                    viewportOffsetY: CGFloat(entity.viewportOffsetY),
                    associatedNoteId: entity.associatedNoteId,
                    updatedAt: updatedAt
                )
            }
        }
    }
    
    public func saveDiagram(_ diagram: LogicBoard) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<DiagramEntity> = NSFetchRequest(entityName: "DiagramEntity")
            request.predicate = NSPredicate(format: "id == %@", diagram.id as CVarArg)
            let entity = try context.fetch(request).first ?? DiagramEntity(context: context)
            
            entity.id = diagram.id
            entity.title = diagram.title
            entity.templateType = diagram.templateType.rawValue
            entity.nodesData = try self.jsonEncoder.encode(diagram.nodes)
            entity.edgesData = try self.jsonEncoder.encode(diagram.edges)
            entity.viewportScale = Double(diagram.viewportScale)
            entity.viewportOffsetX = Double(diagram.viewportOffsetX)
            entity.viewportOffsetY = Double(diagram.viewportOffsetY)
            entity.associatedNoteId = diagram.associatedNoteId
            entity.updatedAt = diagram.updatedAt
            
            try context.save()
        }
    }
    
    public func deleteDiagram(id: UUID) async throws {
        let context = persistenceController.container.newBackgroundContext()
        try await context.perform {
            let request: NSFetchRequest<DiagramEntity> = NSFetchRequest(entityName: "DiagramEntity")
            request.predicate = NSPredicate(format: "id == %@", id as CVarArg)
            if let entity = try context.fetch(request).first {
                context.delete(entity)
                try context.save()
            }
        }
    }
}
