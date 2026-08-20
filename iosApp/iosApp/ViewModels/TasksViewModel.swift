// NoteVault-iOS/ViewModels/TasksViewModel.swift

import Foundation
import Combine

public enum TaskFilterSection: String, CaseIterable {
    case all = "All"
    case today = "Today"
    case upcoming = "Upcoming"
    case completed = "Completed"
}

@MainActor
public final class TasksViewModel: ObservableObject {
    @Published public var tasks: [TaskItem] = []
    @Published public var activeSection: TaskFilterSection = .all
    @Published public var newTaskTitle: String = ""
    @Published public var newTaskPriority: TaskPriority = .medium
    @Published public var newTaskDueDate: Date?
    @Published public var newTaskReminderDate: Date?
    
    private let repository: TaskRepositoryProtocol
    private let notificationManager: NotificationManager
    
    public init(
        repository: TaskRepositoryProtocol = TaskRepository(),
        notificationManager: NotificationManager = .shared
    ) {
        self.repository = repository
        self.notificationManager = notificationManager
    }
    
    public func loadTasks() async {
        do {
            tasks = try await repository.getTasks()
        } catch {
            print("Failed to load tasks: \(error)")
        }
    }
    
    public var filteredTasks: [TaskItem] {
        let calendar = Calendar.current
        switch activeSection {
        case .all:
            return tasks.filter { !$0.isCompleted }
        case .today:
            return tasks.filter { task in
                guard let due = task.dueDate, !task.isCompleted else { return false }
                return calendar.isDateInToday(due)
            }
        case .upcoming:
            return tasks.filter { task in
                guard let due = task.dueDate, !task.isCompleted else { return false }
                return due > Date() && !calendar.isDateInToday(due)
            }
        case .completed:
            return tasks.filter { $0.isCompleted }
        }
    }
    
    public func createTask() async {
        let trimmed = newTaskTitle.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        
        let task = TaskItem(
            title: trimmed,
            dueDate: newTaskDueDate,
            reminderDate: newTaskReminderDate,
            priority: newTaskPriority
        )
        
        do {
            try await repository.saveTask(task)
            if task.reminderDate != nil {
                await notificationManager.scheduleTaskReminder(task: task)
            }
            newTaskTitle = ""
            newTaskDueDate = nil
            newTaskReminderDate = nil
            await loadTasks()
        } catch {
            print("Failed to save task: \(error)")
        }
    }
    
    public func toggleCompletion(for taskId: UUID) async {
        do {
            try await repository.toggleTaskCompletion(id: taskId)
            if let task = tasks.first(where: { $0.id == taskId }), task.isCompleted {
                notificationManager.cancelTaskReminder(taskId: taskId)
            }
            await loadTasks()
        } catch {
            print("Failed to toggle task: \(error)")
        }
    }
    
    public func deleteTask(id: UUID) async {
        do {
            notificationManager.cancelTaskReminder(taskId: id)
            try await repository.deleteTask(id: id)
            await loadTasks()
        } catch {
            print("Failed to delete task: \(error)")
        }
    }
}
