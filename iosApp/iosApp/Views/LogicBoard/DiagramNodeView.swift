// NoteVault-iOS/Views/LogicBoard/DiagramNodeView.swift

import SwiftUI

public struct DiagramNodeView: View {
    public let node: BoardNode
    public let isSelected: Bool
    public let onPositionChanged: (CGPoint) -> Void
    
    @State private var dragOffset: CGSize = .zero
    
    public var body: some View {
        VStack(spacing: 4) {
            Text(node.label)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(Color(hex: node.textColorHex))
                .lineLimit(2)
                .multilineTextAlignment(.center)
            
            if let subtext = node.subtext, !subtext.isEmpty {
                Text(subtext)
                    .font(.caption2)
                    .foregroundColor(Color(hex: node.textColorHex).opacity(0.8))
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .frame(width: node.width, height: node.height)
        .background(shapeBackground)
        .overlay(
            shapeBorder
        )
        .shadow(color: Color.black.opacity(0.12), radius: isSelected ? 8 : 3, x: 0, y: isSelected ? 4 : 1)
        .position(x: node.x + dragOffset.width, y: node.y + dragOffset.height)
        .gesture(
            DragGesture()
                .onChanged { val in
                    dragOffset = val.translation
                }
                .onEnded { val in
                    let finalX = node.x + val.translation.width
                    let finalY = node.y + val.translation.height
                    dragOffset = .zero
                    onPositionChanged(CGPoint(x: max(60, finalX), y: max(40, finalY)))
                }
        )
    }
    
    @ViewBuilder
    private var shapeBackground: some View {
        switch node.shape {
        case .rectangle:
            Rectangle().fill(Color(hex: node.colorHex))
        case .roundedRectangle:
            RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color(hex: node.colorHex))
        case .diamond:
            DiamondShape().fill(Color(hex: node.colorHex))
        case .circle:
            Circle().fill(Color(hex: node.colorHex))
        case .capsule:
            Capsule().fill(Color(hex: node.colorHex))
        case .stickyNote:
            RoundedRectangle(cornerRadius: 4).fill(Color(hex: node.colorHex))
        }
    }
    
    @ViewBuilder
    private var shapeBorder: some View {
        if isSelected {
            switch node.shape {
            case .rectangle:
                Rectangle().stroke(Color.accentColor, lineWidth: 2.5)
            case .roundedRectangle:
                RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Color.accentColor, lineWidth: 2.5)
            case .diamond:
                DiamondShape().stroke(Color.accentColor, lineWidth: 2.5)
            case .circle:
                Circle().stroke(Color.accentColor, lineWidth: 2.5)
            case .capsule:
                Capsule().stroke(Color.accentColor, lineWidth: 2.5)
            case .stickyNote:
                RoundedRectangle(cornerRadius: 4).stroke(Color.accentColor, lineWidth: 2.5)
            }
        }
    }
}

public struct DiamondShape: Shape {
    public func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        path.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.midY))
        path.closeSubpath()
        return path
    }
}
