// NoteVault-iOS/Views/Notes/NoteCardView.swift

import SwiftUI

public struct NoteCardView: View {
    public let note: Note
    public let onTogglePin: () -> Void
    public let onToggleArchive: () -> Void
    public let onDelete: () -> Void
    
    public init(
        note: Note,
        onTogglePin: @escaping () -> Void,
        onToggleArchive: @escaping () -> Void,
        onDelete: @escaping () -> Void
    ) {
        self.note = note
        self.onTogglePin = onTogglePin
        self.onToggleArchive = onToggleArchive
        self.onDelete = onDelete
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Text(note.title.isEmpty ? "Untitled Note" : note.title)
                    .font(.headline)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                
                Spacer()
                
                if note.isPinned {
                    Image(systemName: "pin.fill")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
                
                if note.isLocked {
                    Image(systemName: "lock.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            if note.isLocked {
                HStack(spacing: 6) {
                    Image(systemName: "lock.shield")
                    Text("Encrypted & Locked Note")
                }
                .font(.subheadline)
                .foregroundColor(.secondary)
                .padding(.vertical, 4)
            } else {
                Text(note.previewSnippet)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
            
            HStack {
                Text(note.updatedAt.timeAgoDisplay())
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                if let colorHex = note.colorHex {
                    Circle()
                        .fill(Color(hex: colorHex))
                        .frame(width: 8, height: 8)
                }
            }
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.primary.opacity(0.06), lineWidth: 1)
        )
        .contextMenu {
            Button {
                onTogglePin()
            } label: {
                Label(note.isPinned ? "Unpin Note" : "Pin Note", systemImage: note.isPinned ? "pin.slash" : "pin")
            }
            
            Button {
                onToggleArchive()
            } label: {
                Label(note.isArchived ? "Unarchive" : "Archive", systemImage: note.isArchived ? "tray.and.arrow.up" : "archivebox")
            }
            
            Divider()
            
            Button(role: .destructive) {
                onDelete()
            } label: {
                Label("Delete Note", systemImage: "trash")
            }
        }
    }
}
