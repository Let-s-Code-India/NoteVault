// NoteVault-iOS/Views/Tasks/TasksListView.swift

import SwiftUI

public struct TasksListView: View {
    @StateObject private var viewModel = TasksViewModel()
    @State private var isShowingAddTaskSheet = false
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Section", selection: $viewModel.activeSection) {
                    ForEach(TaskFilterSection.allCases, id: \.self) { section in
                        Text(section.rawValue).tag(section)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                
                if viewModel.filteredTasks.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "checkmark.circle")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No Tasks in \(viewModel.activeSection.rawValue)")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                } else {
                    List {
                        ForEach(viewModel.filteredTasks) { task in
                            TaskRowView(
                                task: task,
                                onToggleCompletion: {
                                    Task { await viewModel.toggleCompletion(for: task.id) }
                                },
                                onDelete: {
                                    Task { await viewModel.deleteTask(id: task.id) }
                                }
                            )
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Tasks & Reminders")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        isShowingAddTaskSheet = true
                    } label: {
                        Image(systemName: "plus")
                            .fontWeight(.semibold)
                    }
                }
            }
            .sheet(isPresented: $isShowingAddTaskSheet) {
                addTaskSheet
            }
            .task {
                await viewModel.loadTasks()
            }
        }
    }
    
    private var addTaskSheet: some View {
        NavigationStack {
            Form {
                Section("Task") {
                    TextField("What needs to be done?", text: $viewModel.newTaskTitle)
                }
                
                Section("Priority") {
                    Picker("Priority", selection: $viewModel.newTaskPriority) {
                        ForEach(TaskPriority.allCases, id: \.self) { p in
                            Text(p.title).tag(p)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                
                Section("Due Date & Reminder") {
                    DatePicker("Due Date", selection: Binding(
                        get: { viewModel.newTaskDueDate ?? Date() },
                        set: { viewModel.newTaskDueDate = $0 }
                    ), displayedComponents: [.date, .hourAndMinute])
                }
            }
            .navigationTitle("New Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { isShowingAddTaskSheet = false }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") {
                        Task {
                            await viewModel.createTask()
                            isShowingAddTaskSheet = false
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(viewModel.newTaskTitle.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}
