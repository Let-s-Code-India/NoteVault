// NoteVault-iOS/Views/LogicBoard/DiagramConnectorView.swift

import SwiftUI

public struct DiagramConnectorView: View {
    public let edge: BoardEdge
    public let sourceNode: BoardNode?
    public let targetNode: BoardNode?
    
    public var body: some View {
        if let src = sourceNode, let dst = targetNode {
            let start = calculatePoint(node: src, anchor: edge.sourceAnchor)
            let end = calculatePoint(node: dst, anchor: edge.targetAnchor)
            
            Path { path in
                path.move(to: start)
                switch edge.style {
                case .straight:
                    path.addLine(to: end)
                case .orthogonal:
                    let midX = (start.x + end.x) / 2
                    path.addLine(to: CGPoint(x: midX, y: start.y))
                    path.addLine(to: CGPoint(x: midX, y: end.y))
                    path.addLine(to: end)
                case .curvedBezier:
                    let control1 = CGPoint(x: start.x + (end.x - start.x) * 0.5, y: start.y)
                    let control2 = CGPoint(x: start.x + (end.x - start.x) * 0.5, y: end.y)
                    path.addCurve(to: end, control1: control1, control2: control2)
                }
            }
            .stroke(Color(hex: edge.colorHex), style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
            
            // Render label if present
            if let label = edge.label, !label.isEmpty {
                let midPoint = CGPoint(x: (start.x + end.x) / 2, y: (start.y + end.y) / 2)
                Text(label)
                    .font(.caption2.weight(.bold))
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color(uiColor: .systemBackground))
                    .foregroundColor(.secondary)
                    .clipShape(Capsule())
                    .position(midPoint)
            }
        }
    }
    
    private func calculatePoint(node: BoardNode, anchor: NodeAnchor) -> CGPoint {
        let x = node.x - (node.width / 2) + (node.width * anchor.x)
        let y = node.y - (node.height / 2) + (node.height * anchor.y)
        return CGPoint(x: x, y: y)
    }
}
