// NoteVault-iOS/Views/LogicBoard/TemplatePickerSheet.swift

import SwiftUI

public struct TemplatePickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    public let onSelectTemplate: (DiagramTemplateType) -> Void
    
    public var body: some View {
        NavigationStack {
            List {
                ForEach(DiagramTemplateType.allCases, id: \.self) { type in
                    Button {
                        onSelectTemplate(type)
                        dismiss()
                    } label: {
                        HStack(spacing: 16) {
                            Image(systemName: type.iconName)
                                .font(.title2)
                                .foregroundColor(.accentColor)
                                .frame(width: 32)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(type.displayName)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                
                                Text(type.descriptionText)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                        }
                        .padding(.vertical, 6)
                    }
                }
            }
            .navigationTitle("Choose Diagram Template")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
