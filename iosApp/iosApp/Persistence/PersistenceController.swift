// NoteVault-iOS/Persistence/PersistenceController.swift

import Foundation
import CoreData

public final class PersistenceController: @unchecked Sendable {
    public static let shared = PersistenceController()
    
    public static var preview: PersistenceController = {
        let controller = PersistenceController(inMemory: true)
        return controller
    }()
    
    public let container: NSPersistentContainer
    
    public init(inMemory: Bool = false) {
        let model = PersistenceController.createManagedObjectModel()
        container = NSPersistentContainer(name: "NoteVault", managedObjectModel: model)
        
        if inMemory {
            container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null")
        } else {
            if let description = container.persistentStoreDescriptions.first {
                description.setOption(
                    FileProtectionType.complete as NSObject,
                    forKey: NSPersistentStoreFileProtectionKey
                )
                description.shouldMigrateStoreAutomatically = true
                description.shouldInferMappingModelAutomatically = true
            }
        }
        
        container.loadPersistentStores { [container] description, error in
            if let error = error as NSError? {
                fatalError("Unresolved Core Data error \(error), \(error.userInfo)")
            }
            container.viewContext.automaticallyMergesChangesFromParent = true
            container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        }
    }
    
    private static func createManagedObjectModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        
        // --- Note Entity ---
        let noteEntity = NSEntityDescription()
        noteEntity.name = "NoteEntity"
        noteEntity.managedObjectClassName = "NoteEntity"
        
        let noteId = NSAttributeDescription()
        noteId.name = "id"
        noteId.attributeType = .UUIDAttributeType
        noteId.isOptional = false
        
        let noteTitle = NSAttributeDescription()
        noteTitle.name = "title"
        noteTitle.attributeType = .stringAttributeType
        noteTitle.isOptional = false
        
        let noteContent = NSAttributeDescription()
        noteContent.name = "content"
        noteContent.attributeType = .stringAttributeType
        noteContent.isOptional = false
        
        let noteFolderId = NSAttributeDescription()
        noteFolderId.name = "folderId"
        noteFolderId.attributeType = .UUIDAttributeType
        noteFolderId.isOptional = true
        
        let noteCreatedAt = NSAttributeDescription()
        noteCreatedAt.name = "createdAt"
        noteCreatedAt.attributeType = .dateAttributeType
        noteCreatedAt.isOptional = false
        
        let noteUpdatedAt = NSAttributeDescription()
        noteUpdatedAt.name = "updatedAt"
        noteUpdatedAt.attributeType = .dateAttributeType
        noteUpdatedAt.isOptional = false
        
        let noteIsPinned = NSAttributeDescription()
        noteIsPinned.name = "isPinned"
        noteIsPinned.attributeType = .booleanAttributeType
        noteIsPinned.defaultValue = false
        
        let noteIsArchived = NSAttributeDescription()
        noteIsArchived.name = "isArchived"
        noteIsArchived.attributeType = .booleanAttributeType
        noteIsArchived.defaultValue = false
        
        let noteIsLocked = NSAttributeDescription()
        noteIsLocked.name = "isLocked"
        noteIsLocked.attributeType = .booleanAttributeType
        noteIsLocked.defaultValue = false
        
        let noteColorHex = NSAttributeDescription()
        noteColorHex.name = "colorHex"
        noteColorHex.attributeType = .stringAttributeType
        noteColorHex.isOptional = true
        
        let noteTagsData = NSAttributeDescription()
        noteTagsData.name = "tagIdsData"
        noteTagsData.attributeType = .binaryDataAttributeType
        noteTagsData.isOptional = true
        
        noteEntity.properties = [
            noteId, noteTitle, noteContent, noteFolderId,
            noteCreatedAt, noteUpdatedAt, noteIsPinned,
            noteIsArchived, noteIsLocked, noteColorHex, noteTagsData
        ]
        
        // --- Folder Entity ---
        let folderEntity = NSEntityDescription()
        folderEntity.name = "FolderEntity"
        folderEntity.managedObjectClassName = "FolderEntity"
        
        let folderId = NSAttributeDescription()
        folderId.name = "id"
        folderId.attributeType = .UUIDAttributeType
        folderId.isOptional = false
        
        let folderName = NSAttributeDescription()
        folderName.name = "name"
        folderName.attributeType = .stringAttributeType
        folderName.isOptional = false
        
        let folderIcon = NSAttributeDescription()
        folderIcon.name = "iconName"
        folderIcon.attributeType = .stringAttributeType
        folderIcon.isOptional = false
        
        let folderColorHex = NSAttributeDescription()
        folderColorHex.name = "colorHex"
        folderColorHex.attributeType = .stringAttributeType
        folderColorHex.isOptional = true
        
        let folderParentId = NSAttributeDescription()
        folderParentId.name = "parentFolderId"
        folderParentId.attributeType = .UUIDAttributeType
        folderParentId.isOptional = true
        
        let folderCreatedAt = NSAttributeDescription()
        folderCreatedAt.name = "createdAt"
        folderCreatedAt.attributeType = .dateAttributeType
        folderCreatedAt.isOptional = false
        
        folderEntity.properties = [
            folderId, folderName, folderIcon, folderColorHex, folderParentId, folderCreatedAt
        ]
        
        // --- Tag Entity ---
        let tagEntity = NSEntityDescription()
        tagEntity.name = "TagEntity"
        tagEntity.managedObjectClassName = "TagEntity"
        
        let tagId = NSAttributeDescription()
        tagId.name = "id"
        tagId.attributeType = .UUIDAttributeType
        tagId.isOptional = false
        
        let tagName = NSAttributeDescription()
        tagName.name = "name"
        tagName.attributeType = .stringAttributeType
        tagName.isOptional = false
        
        let tagColorHex = NSAttributeDescription()
        tagColorHex.name = "colorHex"
        tagColorHex.attributeType = .stringAttributeType
        tagColorHex.isOptional = false
        
        tagEntity.properties = [tagId, tagName, tagColorHex]
        
        // --- Task Entity ---
        let taskEntity = NSEntityDescription()
        taskEntity.name = "TaskEntity"
        taskEntity.managedObjectClassName = "TaskEntity"
        
        let taskId = NSAttributeDescription()
        taskId.name = "id"
        taskId.attributeType = .UUIDAttributeType
        taskId.isOptional = false
        
        let taskNoteId = NSAttributeDescription()
        taskNoteId.name = "noteId"
        taskNoteId.attributeType = .UUIDAttributeType
        taskNoteId.isOptional = true
        
        let taskTitle = NSAttributeDescription()
        taskTitle.name = "title"
        taskTitle.attributeType = .stringAttributeType
        taskTitle.isOptional = false
        
        let taskIsCompleted = NSAttributeDescription()
        taskIsCompleted.name = "isCompleted"
        taskIsCompleted.attributeType = .booleanAttributeType
        taskIsCompleted.defaultValue = false
        
        let taskDueDate = NSAttributeDescription()
        taskDueDate.name = "dueDate"
        taskDueDate.attributeType = .dateAttributeType
        taskDueDate.isOptional = true
        
        let taskReminderDate = NSAttributeDescription()
        taskReminderDate.name = "reminderDate"
        taskReminderDate.attributeType = .dateAttributeType
        taskReminderDate.isOptional = true
        
        let taskPriority = NSAttributeDescription()
        taskPriority.name = "priority"
        taskPriority.attributeType = .integer16AttributeType
        taskPriority.defaultValue = 1
        
        let taskCreatedAt = NSAttributeDescription()
        taskCreatedAt.name = "createdAt"
        taskCreatedAt.attributeType = .dateAttributeType
        taskCreatedAt.isOptional = false
        
        taskEntity.properties = [
            taskId, taskNoteId, taskTitle, taskIsCompleted,
            taskDueDate, taskReminderDate, taskPriority, taskCreatedAt
        ]
        
        // --- Diagram Entity ---
        let diagramEntity = NSEntityDescription()
        diagramEntity.name = "DiagramEntity"
        diagramEntity.managedObjectClassName = "DiagramEntity"
        
        let diagramId = NSAttributeDescription()
        diagramId.name = "id"
        diagramId.attributeType = .UUIDAttributeType
        diagramId.isOptional = false
        
        let diagramTitle = NSAttributeDescription()
        diagramTitle.name = "title"
        diagramTitle.attributeType = .stringAttributeType
        diagramTitle.isOptional = false
        
        let diagramTemplateType = NSAttributeDescription()
        diagramTemplateType.name = "templateType"
        diagramTemplateType.attributeType = .stringAttributeType
        diagramTemplateType.isOptional = false
        
        let diagramNodesData = NSAttributeDescription()
        diagramNodesData.name = "nodesData"
        diagramNodesData.attributeType = .binaryDataAttributeType
        diagramNodesData.isOptional = false
        
        let diagramEdgesData = NSAttributeDescription()
        diagramEdgesData.name = "edgesData"
        diagramEdgesData.attributeType = .binaryDataAttributeType
        diagramEdgesData.isOptional = false
        
        let diagramScale = NSAttributeDescription()
        diagramScale.name = "viewportScale"
        diagramScale.attributeType = .doubleAttributeType
        diagramScale.defaultValue = 1.0
        
        let diagramOffsetX = NSAttributeDescription()
        diagramOffsetX.name = "viewportOffsetX"
        diagramOffsetX.attributeType = .doubleAttributeType
        diagramOffsetX.defaultValue = 0.0
        
        let diagramOffsetY = NSAttributeDescription()
        diagramOffsetY.name = "viewportOffsetY"
        diagramOffsetY.attributeType = .doubleAttributeType
        diagramOffsetY.defaultValue = 0.0
        
        let diagramNoteId = NSAttributeDescription()
        diagramNoteId.name = "associatedNoteId"
        diagramNoteId.attributeType = .UUIDAttributeType
        diagramNoteId.isOptional = true
        
        let diagramUpdatedAt = NSAttributeDescription()
        diagramUpdatedAt.name = "updatedAt"
        diagramUpdatedAt.attributeType = .dateAttributeType
        diagramUpdatedAt.isOptional = false
        
        diagramEntity.properties = [
            diagramId, diagramTitle, diagramTemplateType,
            diagramNodesData, diagramEdgesData, diagramScale,
            diagramOffsetX, diagramOffsetY, diagramNoteId, diagramUpdatedAt
        ]
        
        model.entities = [noteEntity, folderEntity, tagEntity, taskEntity, diagramEntity]
        return model
    }
}
