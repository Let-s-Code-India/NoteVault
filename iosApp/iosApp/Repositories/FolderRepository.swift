// NoteVault-iOS/Repositories/FolderRepository.swift

import Foundation

public protocol FolderRepositoryProtocol: Sendable {
    func getFolders() async throws -> [Folder]
    func saveFolder(_ folder: Folder) async throws
    func deleteFolder(id: UUID) async throws
}

public final class FolderRepository: FolderRepositoryProtocol {
    private let dao: NoteVaultDAOSwift
    
    public init(dao: NoteVaultDAOSwift = NoteVaultDAO()) {
        self.dao = dao
    }
    
    public func getFolders() async throws -> [Folder] {
        return try await dao.fetchFolders()
    }
    
    public func saveFolder(_ folder: Folder) async throws {
        try await dao.saveFolder(folder)
    }
    
    public func deleteFolder(id: UUID) async throws {
        try await dao.deleteFolder(id: id)
    }
}
