// NoteVault-iOS/Repositories/TaskRepository.swift

import Foundation

public protocol TaskRepositoryProtocol: Sendable {
    func getTasks() async throws -> [TaskItem]
    func saveTask(_ task: TaskItem) async throws
    func toggleTaskCompletion(id: UUID) async throws
    func deleteTask(id: UUID) async throws
}

public final class TaskRepository: TaskRepositoryProtocol {
    private let dao: NoteVaultDAOSwift
    
    public init(dao: NoteVaultDAOSwift = NoteVaultDAO()) {
        self.dao = dao
    }
    
    public func getTasks() async throws -> [TaskItem] {
        return try await dao.fetchTasks()
    }
    
    public func saveTask(_ task: TaskItem) async throws {
        try await dao.saveTask(task)
    }
    
    public func toggleTaskCompletion(id: UUID) async throws {
        let tasks = try await dao.fetchTasks()
        if var task = tasks.first(where: { $0.id == id }) {
            task.isCompleted.toggle()
            try await dao.saveTask(task)
        }
    }
    
    public func deleteTask(id: UUID) async throws {
        try await dao.deleteTask(id: id)
    }
}
