// NoteVault-iOS/ViewModels/FoldersTagsViewModel.swift

import Foundation
import Combine

@MainActor
public final class FoldersTagsViewModel: ObservableObject {
    @Published public var folders: [Folder] = []
    @Published public var tags: [Tag] = []
    @Published public var selectedFolder: Folder?
    @Published public var selectedTag: Tag?
    @Published public var isPresentingCreateFolder: Bool = false
    @Published public var isPresentingCreateTag: Bool = false
    
    private let folderRepository: FolderRepositoryProtocol
    private let tagRepository: TagRepositoryProtocol
    
    public init(
        folderRepository: FolderRepositoryProtocol = FolderRepository(),
        tagRepository: TagRepositoryProtocol = TagRepository()
    ) {
        self.folderRepository = folderRepository
        self.tagRepository = tagRepository
    }
    
    public func loadData() async {
        do {
            folders = try await folderRepository.getFolders()
            tags = try await tagRepository.getTags()
        } catch {
            print("Failed to load folders/tags: \(error)")
        }
    }
    
    public func createFolder(name: String, iconName: String = "folder.fill", colorHex: String? = nil) async {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        let folder = Folder(name: name, iconName: iconName, colorHex: colorHex)
        do {
            try await folderRepository.saveFolder(folder)
            await loadData()
        } catch {
            print("Failed to create folder: \(error)")
        }
    }
    
    public func deleteFolder(id: UUID) async {
        do {
            try await folderRepository.deleteFolder(id: id)
            await loadData()
        } catch {
            print("Failed to delete folder: \(error)")
        }
    }
    
    public func createTag(name: String, colorHex: String = "#0A84FF") async {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        let tag = Tag(name: name, colorHex: colorHex)
        do {
            try await tagRepository.saveTag(tag)
            await loadData()
        } catch {
            print("Failed to create tag: \(error)")
        }
    }
    
    public func deleteTag(id: UUID) async {
        do {
            try await tagRepository.deleteTag(id: id)
            await loadData()
        } catch {
            print("Failed to delete tag: \(error)")
        }
    }
}
