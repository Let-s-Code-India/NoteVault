// NoteVault-iOS/Views/Search/GlobalSearchView.swift

import SwiftUI

public struct GlobalSearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            List {
                if !viewModel.results.notes.isEmpty {
                    Section("Notes") {
                        ForEach(viewModel.results.notes) { note in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(note.title.isEmpty ? "Untitled Note" : note.title)
                                    .font(.headline)
                                Text(note.previewSnippet)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                
                if !viewModel.results.folders.isEmpty {
                    Section("Folders") {
                        ForEach(viewModel.results.folders) { folder in
                            Label(folder.name, systemImage: folder.iconName)
                        }
                    }
                }
                
                if !viewModel.results.tags.isEmpty {
                    Section("Tags") {
                        ForEach(viewModel.results.tags) { tag in
                            Label(tag.name, systemImage: "tag.fill")
                                .foregroundColor(Color(hex: tag.colorHex))
                        }
                    }
                }
                
                if !viewModel.results.tasks.isEmpty {
                    Section("Tasks") {
                        ForEach(viewModel.results.tasks) { task in
                            Text(task.title)
                        }
                    }
                }
            }
            .navigationTitle("Search")
            .searchable(text: $viewModel.searchQuery, prompt: "Search notes, folders, tags, tasks...")
            .onChange(of: viewModel.searchQuery) { _ in
                Task {
                    await viewModel.performSearch()
                }
            }
        }
    }
}
