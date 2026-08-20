// NoteVault-iOS/ViewModels/NoteEditorViewModel.swift

import Foundation
import Combine

@MainActor
public final class NoteEditorViewModel: ObservableObject {
    @Published public var note: Note
    @Published public var availableFolders: [Folder] = []
    @Published public var availableTags: [Tag] = []
    @Published public var isLockedSessionUnlocked: Bool = false
    @Published public var hasUnsavedChanges: Bool = false
    @Published public var isSaving: Bool = false
    
    private let noteRepository: NoteRepositoryProtocol
    private let folderRepository: FolderRepositoryProtocol
    private let tagRepository: TagRepositoryProtocol
    
    public init(
        note: Note? = nil,
        noteRepository: NoteRepositoryProtocol = NoteRepository(),
        folderRepository: FolderRepositoryProtocol = FolderRepository(),
        tagRepository: TagRepositoryProtocol = TagRepository()
    ) {
        self.note = note ?? Note(title: "", content: "")
        self.noteRepository = noteRepository
        self.folderRepository = folderRepository
        self.tagRepository = tagRepository
        
        // If note is not locked, session is already unlocked
        if !(note?.isLocked ?? false) {
            self.isLockedSessionUnlocked = true
        }
    }
    
    public func loadMetadata() async {
        do {
            self.availableFolders = try await folderRepository.getFolders()
            self.availableTags = try await tagRepository.getTags()
        } catch {
            print("Failed to load folder/tag metadata: \(error)")
        }
    }
    
    public func updateTitle(_ newTitle: String) {
        guard note.title != newTitle else { return }
        note.title = newTitle
        hasUnsavedChanges = true
    }
    
    public func updateContent(_ newContent: String) {
        guard note.content != newContent else { return }
        note.content = newContent
        hasUnsavedChanges = true
    }
    
    public func setFolder(_ folderId: UUID?) {
        note.folderId = folderId
        hasUnsavedChanges = true
    }
    
    public func toggleTag(_ tagId: UUID) {
        if note.tagIds.contains(tagId) {
            note.tagIds.remove(tagId)
        } else {
            note.tagIds.insert(tagId)
        }
        hasUnsavedChanges = true
    }
    
    public func toggleLock() {
        note.isLocked.toggle()
        if note.isLocked {
            isLockedSessionUnlocked = false
        }
        hasUnsavedChanges = true
    }
    
    public func saveNote() async {
        guard hasUnsavedChanges || note.title.isEmpty == false || note.content.isEmpty == false else { return }
        isSaving = true
        do {
            try await noteRepository.saveNote(note)
            hasUnsavedChanges = false
        } catch {
            print("Error saving note: \(error)")
        }
        isSaving = false
    }
}
