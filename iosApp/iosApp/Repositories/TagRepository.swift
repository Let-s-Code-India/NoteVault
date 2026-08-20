// NoteVault-iOS/Repositories/TagRepository.swift

import Foundation

public protocol TagRepositoryProtocol: Sendable {
    func getTags() async throws -> [Tag]
    func saveTag(_ tag: Tag) async throws
    func deleteTag(id: UUID) async throws
}

public final class TagRepository: TagRepositoryProtocol {
    private let dao: NoteVaultDAOSwift
    
    public init(dao: NoteVaultDAOSwift = NoteVaultDAO()) {
        self.dao = dao
    }
    
    public func getTags() async throws -> [Tag] {
        return try await dao.fetchTags()
    }
    
    public func saveTag(_ tag: Tag) async throws {
        try await dao.saveTag(tag)
    }
    
    public func deleteTag(id: UUID) async throws {
        try await dao.deleteTag(id: id)
    }
}
