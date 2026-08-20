// NoteVault-iOS/ViewModels/SearchViewModel.swift

import Foundation
import Combine

public struct SearchResultGroup: Sendable {
    public let notes: [Note]
    public let folders: [Folder]
    public let tags: [Tag]
    public let tasks: [TaskItem]
}

@MainActor
public final class SearchViewModel: ObservableObject {
    @Published public var searchQuery: String = ""
    @Published public var isSearching: Bool = false
    @Published public var results: SearchResultGroup = SearchResultGroup(notes: [], folders: [], tags: [], tasks: [])
    
    private let noteRepo: NoteRepositoryProtocol
    private let folderRepo: FolderRepositoryProtocol
    private let tagRepo: TagRepositoryProtocol
    private let taskRepo: TaskRepositoryProtocol
    
    public init(
        noteRepo: NoteRepositoryProtocol = NoteRepository(),
        folderRepo: FolderRepositoryProtocol = FolderRepository(),
        tagRepo: TagRepositoryProtocol = TagRepository(),
        taskRepo: TaskRepositoryProtocol = TaskRepository()
    ) {
        self.noteRepo = noteRepo
        self.folderRepo = folderRepo
        self.tagRepo = tagRepo
        self.taskRepo = taskRepo
    }
    
    public func performSearch() async {
        let query = searchQuery.trimmingCharacters(in: .whitespaces).lowercased()
        guard !query.isEmpty else {
            results = SearchResultGroup(notes: [], folders: [], tags: [], tasks: [])
            return
        }
        
        isSearching = true
        do {
            async let notesFetch = noteRepo.searchNotes(query: query)
            async let foldersFetch = folderRepo.getFolders()
            async let tagsFetch = tagRepo.getTags()
            async let tasksFetch = taskRepo.getTasks()
            
            let (notes, allFolders, allTags, allTasks) = try await (notesFetch, foldersFetch, tagsFetch, tasksFetch)
            
            let matchedFolders = allFolders.filter { $0.name.lowercased().contains(query) }
            let matchedTags = allTags.filter { $0.name.lowercased().contains(query) }
            let matchedTasks = allTasks.filter { $0.title.lowercased().contains(query) }
            
            self.results = SearchResultGroup(
                notes: notes,
                folders: matchedFolders,
                tags: matchedTags,
                tasks: matchedTasks
            )
        } catch {
            print("Search failed: \(error)")
        }
        isSearching = false
    }
}
