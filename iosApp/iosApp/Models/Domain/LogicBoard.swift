// NoteVault-iOS/Models/Domain/LogicBoard.swift

import Foundation
import CoreGraphics

public enum NodeShapeType: String, Codable, Sendable {
    case rectangle
    case roundedRectangle
    case diamond
    case circle
    case capsule
    case stickyNote
}

public struct NodeAnchor: Hashable, Codable, Sendable {
    public let x: CGFloat // Normalized 0.0 ... 1.0 relative to node bounds
    public let y: CGFloat // Normalized 0.0 ... 1.0 relative to node bounds
    
    public static let top = NodeAnchor(x: 0.5, y: 0.0)
    public static let bottom = NodeAnchor(x: 0.5, y: 1.0)
    public static let left = NodeAnchor(x: 0.0, y: 0.5)
    public static let right = NodeAnchor(x: 1.0, y: 0.5)
    
    public init(x: CGFloat, y: CGFloat) {
        self.x = x
        self.y = y
    }
}

public struct BoardNode: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var label: String
    public var subtext: String?
    public var x: CGFloat
    public var y: CGFloat
    public var width: CGFloat
    public var height: CGFloat
    public var shape: NodeShapeType
    public var colorHex: String
    public var textColorHex: String
    
    public init(
        id: UUID = UUID(),
        label: String,
        subtext: String? = nil,
        x: CGFloat,
        y: CGFloat,
        width: CGFloat = 160,
        height: CGFloat = 80,
        shape: NodeShapeType = .roundedRectangle,
        colorHex: String = "#1E293B",
        textColorHex: String = "#FFFFFF"
    ) {
        self.id = id
        self.label = label
        self.subtext = subtext
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.shape = shape
        self.colorHex = colorHex
        self.textColorHex = textColorHex
    }
}

public enum ConnectorStyle: String, Codable, Sendable {
    case straight
    case orthogonal
    case curvedBezier
}

public struct BoardEdge: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var sourceNodeId: UUID
    public var targetNodeId: UUID
    public var sourceAnchor: NodeAnchor
    public var targetAnchor: NodeAnchor
    public var label: String?
    public var style: ConnectorStyle
    public var colorHex: String
    public var hasArrow: Bool
    
    public init(
        id: UUID = UUID(),
        sourceNodeId: UUID,
        targetNodeId: UUID,
        sourceAnchor: NodeAnchor = .right,
        targetAnchor: NodeAnchor = .left,
        label: String? = nil,
        style: ConnectorStyle = .orthogonal,
        colorHex: String = "#64748B",
        hasArrow: Bool = true
    ) {
        self.id = id
        self.sourceNodeId = sourceNodeId
        self.targetNodeId = targetNodeId
        self.sourceAnchor = sourceAnchor
        self.targetAnchor = targetAnchor
        self.label = label
        self.style = style
        self.colorHex = colorHex
        self.hasArrow = hasArrow
    }
}

public struct LogicBoard: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var title: String
    public var templateType: DiagramTemplateType
    public var nodes: [BoardNode]
    public var edges: [BoardEdge]
    public var viewportScale: CGFloat
    public var viewportOffsetX: CGFloat
    public var viewportOffsetY: CGFloat
    public var associatedNoteId: UUID?
    public var updatedAt: Date
    
    public init(
        id: UUID = UUID(),
        title: String = "Untitled Diagram",
        templateType: DiagramTemplateType = .freeform,
        nodes: [BoardNode] = [],
        edges: [BoardEdge] = [],
        viewportScale: CGFloat = 1.0,
        viewportOffsetX: CGFloat = 0.0,
        viewportOffsetY: CGFloat = 0.0,
        associatedNoteId: UUID? = nil,
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.title = title
        self.templateType = templateType
        self.nodes = nodes
        self.edges = edges
        self.viewportScale = viewportScale
        self.viewportOffsetX = viewportOffsetX
        self.viewportOffsetY = viewportOffsetY
        self.associatedNoteId = associatedNoteId
        self.updatedAt = updatedAt
    }
}
