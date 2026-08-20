// NoteVault-iOS/Views/Tasks/TaskRowView.swift

import SwiftUI

public struct TaskRowView: View {
    public let task: TaskItem
    public let onToggleCompletion: () -> Void
    public let onDelete: () -> Void
    
    public var body: some View {
        HStack(spacing: 12) {
            Button {
                onToggleCompletion()
            } label: {
                Image(systemName: task.isCompleted ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundColor(task.isCompleted ? .green : .secondary)
            }
            .buttonStyle(.plain)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(task.title)
                    .font(.body)
                    .strikethrough(task.isCompleted, color: .secondary)
                    .foregroundColor(task.isCompleted ? .secondary : .primary)
                
                HStack(spacing: 8) {
                    if let due = task.dueDate {
                        HStack(spacing: 3) {
                            Image(systemName: "calendar")
                            Text(due.formattedDate())
                        }
                        .font(.caption2)
                        .foregroundColor(task.isOverdue ? .red : .secondary)
                    }
                    
                    Circle()
                        .fill(Color(hex: task.priority.colorHex))
                        .frame(width: 6, height: 6)
                    
                    Text(task.priority.title)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
        }
        .padding(.vertical, 6)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                onDelete()
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }
}
