// NoteVault-iOS/Views/FoldersAndTags/TagCloudView.swift

import SwiftUI

public struct TagCloudView: View {
    @ObservedObject var viewModel: FoldersTagsViewModel
    @State private var newTagName = ""
    @State private var isShowingCreateTagAlert = false
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if viewModel.tags.isEmpty {
                Text("No tags yet")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(viewModel.tags) { tag in
                            HStack(spacing: 4) {
                                Text(tag.name)
                                    .font(.caption.weight(.medium))
                                
                                Button {
                                    Task { await viewModel.deleteTag(id: tag.id) }
                                } label: {
                                    Image(systemName: "xmark")
                                        .font(.system(size: 8, weight: .bold))
                                }
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color(hex: tag.colorHex).opacity(0.15))
                            .foregroundColor(Color(hex: tag.colorHex))
                            .clipShape(Capsule())
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            
            Button {
                isShowingCreateTagAlert = true
            } label: {
                Label("Add Tag", systemImage: "tag.badge.plus")
                    .font(.subheadline)
                    .foregroundColor(.accentColor)
            }
            .padding(.top, 4)
        }
        .alert("New Tag", isPresented: $isShowingCreateTagAlert) {
            TextField("Tag name (e.g. Work, Ideas)", text: $newTagName)
            Button("Cancel", role: .cancel) { newTagName = "" }
            Button("Add") {
                Task {
                    await viewModel.createTag(name: newTagName)
                    newTagName = ""
                }
            }
        }
    }
}
