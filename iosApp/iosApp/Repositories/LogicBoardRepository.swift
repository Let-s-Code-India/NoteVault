// NoteVault-iOS/Repositories/LogicBoardRepository.swift

import Foundation
import CoreGraphics

public protocol LogicBoardRepositoryProtocol: Sendable {
    func getDiagrams() async throws -> [LogicBoard]
    func getDiagram(id: UUID) async throws -> LogicBoard?
    func saveDiagram(_ diagram: LogicBoard) async throws
    func deleteDiagram(id: UUID) async throws
    func createDiagramFromTemplate(type: DiagramTemplateType, title: String) async throws -> LogicBoard
}

public final class LogicBoardRepository: LogicBoardRepositoryProtocol {
    private let dao: NoteVaultDAOSwift
    
    public init(dao: NoteVaultDAOSwift = NoteVaultDAO()) {
        self.dao = dao
    }
    
    public func getDiagrams() async throws -> [LogicBoard] {
        return try await dao.fetchDiagrams()
    }
    
    public func getDiagram(id: UUID) async throws -> LogicBoard? {
        let diagrams = try await dao.fetchDiagrams()
        return diagrams.first(where: { $0.id == id })
    }
    
    public func saveDiagram(_ diagram: LogicBoard) async throws {
        var updated = diagram
        updated.updatedAt = Date()
        try await dao.saveDiagram(updated)
    }
    
    public func deleteDiagram(id: UUID) async throws {
        try await dao.deleteDiagram(id: id)
    }
    
    public func createDiagramFromTemplate(type: DiagramTemplateType, title: String) async throws -> LogicBoard {
        let diagram = LocalDiagramGenerator.generateTemplateDiagram(type: type, title: title)
        try await dao.saveDiagram(diagram)
        return diagram
    }
}
