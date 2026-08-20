// NoteVault-iOS/Views/Notes/NotesListView.swift

import SwiftUI

public struct NotesListView: View {
    @StateObject private var viewModel = NotesListViewModel()
    @State private var isCreatingNote = false
    @State private var selectedNoteForEditing: Note?
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            ZStack {
                Color(uiColor: .systemGroupedBackground)
                    .ignoresSafeArea()
                
                if viewModel.isLoading && viewModel.filteredNotes.isEmpty {
                    ProgressView()
                } else if viewModel.filteredNotes.isEmpty {
                    emptyStateView
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.filteredNotes) { note in
                                Button {
                                    selectedNoteForEditing = note
                                } label: {
                                    NoteCardView(
                                        note: note,
                                        onTogglePin: {
                                            Task { await viewModel.togglePin(for: note.id) }
                                        },
                                        onToggleArchive: {
                                            Task { await viewModel.toggleArchive(for: note.id) }
                                        },
                                        onDelete: {
                                            Task { await viewModel.deleteNote(id: note.id) }
                                        }
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 12)
                        .padding(.bottom, 80)
                    }
                }
            }
            .navigationTitle("Notes")
            .searchable(text: $viewModel.searchQuery, prompt: "Search notes...")
            .onChange(of: viewModel.searchQuery) { _ in
                viewModel.applyFilteringAndSorting()
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Menu {
                        Picker("Sort By", selection: $viewModel.sortOption) {
                            ForEach(NoteSortOption.allCases, id: \.self) { option in
                                Text(option.rawValue).tag(option)
                            }
                        }
                    } label: {
                        Image(systemName: "arrow.up.arrow.down.circle")
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        selectedNoteForEditing = Note(title: "", content: "")
                    } label: {
                        Image(systemName: "square.and.pencil")
                            .fontWeight(.semibold)
                    }
                }
            }
            .task {
                await viewModel.loadNotes()
            }
            .sheet(item: $selectedNoteForEditing) { note in
                NoteEditorView(note: note) {
                    Task { await viewModel.loadNotes() }
                }
            }
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "note.text")
                .font(.system(size: 52))
                .foregroundColor(.secondary)
            
            Text("No Notes Found")
                .font(.title3)
                .fontWeight(.semibold)
            
            Text("Tap the pen icon to create your first encrypted offline note.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            Button {
                selectedNoteForEditing = Note(title: "", content: "")
            } label: {
                Label("Create Note", systemImage: "plus")
                    .fontWeight(.medium)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .clipShape(Capsule())
            }
            .padding(.top, 8)
        }
    }
}
