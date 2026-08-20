// NoteVault-iOS/Views/Notes/NoteEditorView.swift

import SwiftUI

public struct NoteEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel: NoteEditorViewModel
    private let onSaveCompletion: () -> Void
    
    public init(note: Note, onSaveCompletion: @escaping () -> Void = {}) {
        _viewModel = StateObject(wrappedValue: NoteEditorViewModel(note: note))
        self.onSaveCompletion = onSaveCompletion
    }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                Color(uiColor: .systemBackground)
                    .ignoresSafeArea()
                
                if viewModel.note.isLocked && !viewModel.isLockedSessionUnlocked {
                    lockedShieldView
                } else {
                    VStack(spacing: 0) {
                        // Title Input
                        TextField("Note Title", text: Binding(
                            get: { viewModel.note.title },
                            set: { viewModel.updateTitle($0) }
                        ))
                        .font(.title2.weight(.bold))
                        .padding(.horizontal, 18)
                        .padding(.top, 16)
                        .padding(.bottom, 8)
                        
                        // Tag chips selector
                        if !viewModel.availableTags.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(viewModel.availableTags) { tag in
                                        let isSelected = viewModel.note.tagIds.contains(tag.id)
                                        Button {
                                            viewModel.toggleTag(tag.id)
                                        } label: {
                                            Text(tag.name)
                                                .font(.caption.weight(.medium))
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 5)
                                                .background(isSelected ? Color(hex: tag.colorHex) : Color(uiColor: .secondarySystemFill))
                                                .foregroundColor(isSelected ? .white : .primary)
                                                .clipShape(Capsule())
                                        }
                                    }
                                }
                                .padding(.horizontal, 18)
                                .padding(.vertical, 4)
                            }
                        }
                        
                        Divider()
                            .padding(.top, 8)
                        
                        // Markdown / Content Editor
                        TextEditor(text: Binding(
                            get: { viewModel.note.content },
                            set: { viewModel.updateContent($0) }
                        ))
                        .font(.body)
                        .padding(.horizontal, 14)
                        .padding(.top, 8)
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") {
                        Task {
                            await viewModel.saveNote()
                            onSaveCompletion()
                            dismiss()
                        }
                    }
                    .fontWeight(.semibold)
                }
                
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.toggleLock()
                    } label: {
                        Image(systemName: viewModel.note.isLocked ? "lock.fill" : "lock.open")
                            .foregroundColor(viewModel.note.isLocked ? .orange : .primary)
                    }
                    
                    Menu {
                        Section("Folder") {
                            Button("No Folder") {
                                viewModel.setFolder(nil)
                            }
                            ForEach(viewModel.availableFolders) { folder in
                                Button {
                                    viewModel.setFolder(folder.id)
                                } label: {
                                    Label(folder.name, systemImage: folder.iconName)
                                }
                            }
                        }
                    } label: {
                        Image(systemName: "folder")
                    }
                }
            }
            .task {
                await viewModel.loadMetadata()
            }
        }
    }
    
    private var lockedShieldView: some View {
        VStack(spacing: 20) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 64))
                .foregroundColor(.orange)
            
            Text("Locked Note")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("This note is protected with biometric security.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button {
                Task {
                    let res = await BiometricAuthManager.shared.authenticate(reason: "Unlock Note")
                    if res.success {
                        viewModel.isLockedSessionUnlocked = true
                    }
                }
            } label: {
                Label("Authenticate to View", systemImage: "faceid")
                    .fontWeight(.semibold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .clipShape(Capsule())
            }
            .padding(.top, 12)
        }
        .padding(32)
    }
}
