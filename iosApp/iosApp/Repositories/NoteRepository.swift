// NoteVault-iOS/Repositories/NoteRepository.swift

import Foundation
import Combine

public protocol NoteRepositoryProtocol: Sendable {
    func getNotes() async throws -> [Note]
    func getNote(id: UUID) async throws -> Note?
    func saveNote(_ note: Note) async throws
    func deleteNote(id: UUID) async throws
    func togglePin(id: UUID) async throws
    func toggleArchive(id: UUID) async throws
    func searchNotes(query: String) async throws -> [Note]
}

public final class NoteRepository: NoteRepositoryProtocol {
    private let dao: NoteVaultDAOSwift
    
    public init(dao: NoteVaultDAOSwift = NoteVaultDAO()) {
        self.dao = dao
    }
    
    public func getNotes() async throws -> [Note] {
        return try await dao.fetchNotes()
    }
    
    public func getNote(id: UUID) async throws -> Note? {
        let notes = try await dao.fetchNotes()
        return notes.first(where: { $0.id == id })
    }
    
    public func saveNote(_ note: Note) async throws {
        var updated = note
        updated.updatedAt = Date()
        try await dao.saveNote(updated)
    }
    
    public func deleteNote(id: UUID) async throws {
        try await dao.deleteNote(id: id)
    }
    
    public func togglePin(id: UUID) async throws {
        if var note = try await getNote(id: id) {
            note.isPinned.toggle()
            try await saveNote(note)
        }
    }
    
    public func toggleArchive(id: UUID) async throws {
        if var note = try await getNote(id: id) {
            note.isArchived.toggle()
            try await saveNote(note)
        }
    }
    
    public func searchNotes(query: String) async throws -> [Note] {
        let allNotes = try await dao.fetchNotes()
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            return allNotes
        }
        let term = query.lowercased()
        return allNotes.filter { note in
            note.title.localizedCaseInsensitiveContains(term) ||
            note.content.localizedCaseInsensitiveContains(term)
        }
    }
}
