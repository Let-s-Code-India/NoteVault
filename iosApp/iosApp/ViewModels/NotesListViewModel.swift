// NoteVault-iOS/ViewModels/NotesListViewModel.swift

import Foundation
import Combine

public enum NoteFilter: Equatable {
    case all
    case folder(UUID)
    case tag(UUID)
    case archived
}

public enum NoteSortOption: String, CaseIterable {
    case updatedDesc = "Recently Updated"
    case createdDesc = "Recently Created"
    case titleAsc = "Title (A-Z)"
}

@MainActor
public final class NotesListViewModel: ObservableObject {
    @Published public var notes: [Note] = []
    @Published public var filteredNotes: [Note] = []
    @Published public var activeFilter: NoteFilter = .all
    @Published public var sortOption: NoteSortOption = .updatedDesc
    @Published public var searchQuery: String = ""
    @Published public var isLoading: Bool = false
    @Published public var errorMessage: String?
    
    private let noteRepository: NoteRepositoryProtocol
    private let folderRepository: FolderRepositoryProtocol
    private let tagRepository: TagRepositoryProtocol
    
    public init(
        noteRepository: NoteRepositoryProtocol = NoteRepository(),
        folderRepository: FolderRepositoryProtocol = FolderRepository(),
        tagRepository: TagRepositoryProtocol = TagRepository()
    ) {
        self.noteRepository = noteRepository
        self.folderRepository = folderRepository
        self.tagRepository = tagRepository
    }
    
    public func loadNotes() async {
        isLoading = true
        errorMessage = nil
        do {
            notes = try await noteRepository.getNotes()
            applyFilteringAndSorting()
        } catch {
            errorMessage = "Failed to load notes: \(error.localizedDescription)"
        }
        isLoading = false
    }
    
    public func setFilter(_ filter: NoteFilter) {
        activeFilter = filter
        applyFilteringAndSorting()
    }
    
    public func setSort(_ sort: NoteSortOption) {
        sortOption = sort
        applyFilteringAndSorting()
    }
    
    public func togglePin(for noteId: UUID) async {
        do {
            try await noteRepository.togglePin(id: noteId)
            await loadNotes()
        } catch {
            errorMessage = "Could not update pin state."
        }
    }
    
    public func toggleArchive(for noteId: UUID) async {
        do {
            try await noteRepository.toggleArchive(id: noteId)
            await loadNotes()
        } catch {
            errorMessage = "Could not update archive state."
        }
    }
    
    public func deleteNote(id: UUID) async {
        do {
            try await noteRepository.deleteNote(id: id)
            await loadNotes()
        } catch {
            errorMessage = "Could not delete note."
        }
    }
    
    public func applyFilteringAndSorting() {
        var result = notes
        
        // 1. Filter
        switch activeFilter {
        case .all:
            result = result.filter { !$0.isArchived }
        case .folder(let folderId):
            result = result.filter { $0.folderId == folderId && !$0.isArchived }
        case .tag(let tagId):
            result = result.filter { $0.tagIds.contains(tagId) && !$0.isArchived }
        case .archived:
            result = result.filter { $0.isArchived }
        }
        
        // 2. Search
        if !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty {
            let q = searchQuery.lowercased()
            result = result.filter {
                $0.title.lowercased().contains(q) || $0.content.lowercased().contains(q)
            }
        }
        
        // 3. Sort (pinned always on top unless in archive)
        result.sort { a, b in
            if a.isPinned != b.isPinned && activeFilter != .archived {
                return a.isPinned && !b.isPinned
            }
            switch sortOption {
            case .updatedDesc:
                return a.updatedAt > b.updatedAt
            case .createdDesc:
                return a.createdAt > b.createdAt
            case .titleAsc:
                return a.title.localizedCaseInsensitiveCompare(b.title) == .orderedAscending
            }
        }
        
        filteredNotes = result
    }
}
