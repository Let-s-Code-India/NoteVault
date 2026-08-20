// NoteVault-iOS/Views/FoldersAndTags/FolderSidebarView.swift

import SwiftUI

public struct FolderSidebarView: View {
    @StateObject private var viewModel = FoldersTagsViewModel()
    @State private var newFolderName: String = ""
    @State private var isShowingCreateFolderAlert = false
    
    public init() {}
    
    public var body: some View {
        List {
            Section("All Notes") {
                NavigationLink(destination: NotesListView()) {
                    Label("All Notes", systemImage: "tray.full")
                }
            }
            
            Section("Folders") {
                ForEach(viewModel.folders) { folder in
                    HStack {
                        Label(folder.name, systemImage: folder.iconName)
                        Spacer()
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        Button(role: .destructive) {
                            Task { await viewModel.deleteFolder(id: folder.id) }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
                
                Button {
                    isShowingCreateFolderAlert = true
                } label: {
                    Label("New Folder", systemImage: "folder.badge.plus")
                        .foregroundColor(.accentColor)
                }
            }
            
            Section("Tags") {
                TagCloudView(viewModel: viewModel)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Folders & Tags")
        .task {
            await viewModel.loadData()
        }
        .alert("New Folder", isPresented: $isShowingCreateFolderAlert) {
            TextField("Folder Name", text: $newFolderName)
            Button("Cancel", role: .cancel) { newFolderName = "" }
            Button("Create") {
                Task {
                    await viewModel.createFolder(name: newFolderName)
                    newFolderName = ""
                }
            }
        }
    }
}
