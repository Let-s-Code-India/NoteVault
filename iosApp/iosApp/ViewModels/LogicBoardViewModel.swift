// NoteVault-iOS/ViewModels/LogicBoardViewModel.swift

import Foundation
import CoreGraphics
import Combine

public enum CanvasToolMode: String, CaseIterable {
    case select = "arrow.up.left.and.arrow.down.right"
    case addNode = "plus.rectangle.on.rectangle"
    case connect = "arrowshape.turn.up.right"
    case delete = "trash"
}

@MainActor
public final class LogicBoardViewModel: ObservableObject {
    @Published public var board: LogicBoard
    @Published public var selectedNodeId: UUID?
    @Published public var connectingSourceNodeId: UUID?
    @Published public var activeTool: CanvasToolMode = .select
    @Published public var isPresentingTemplatePicker: Bool = false
    
    // Undo / Redo history stacks
    private var undoStack: [LogicBoard] = []
    private var redoStack: [LogicBoard] = []
    
    private let repository: LogicBoardRepositoryProtocol
    
    public init(
        board: LogicBoard? = nil,
        repository: LogicBoardRepositoryProtocol = LogicBoardRepository()
    ) {
        self.board = board ?? LogicBoard()
        self.repository = repository
    }
    
    public func recordSnapshot() {
        undoStack.append(board)
        redoStack.removeAll()
        if undoStack.count > 30 {
            undoStack.removeFirst()
        }
    }
    
    public func undo() {
        guard let previous = undoStack.popLast() else { return }
        redoStack.append(board)
        board = previous
    }
    
    public func redo() {
        guard let next = redoStack.popLast() else { return }
        undoStack.append(board)
        board = next
    }
    
    public func addNode(
        label: String,
        subtext: String? = nil,
        at point: CGPoint,
        shape: NodeShapeType = .roundedRectangle,
        colorHex: String = "#1E293B"
    ) {
        recordSnapshot()
        let newNode = BoardNode(
            label: label,
            subtext: subtext,
            x: point.x,
            y: point.y,
            shape: shape,
            colorHex: colorHex
        )
        board.nodes.append(newNode)
        selectedNodeId = newNode.id
    }
    
    public func updateNodePosition(id: UUID, newPosition: CGPoint) {
        if let index = board.nodes.firstIndex(where: { $0.id == id }) {
            board.nodes[index].x = newPosition.x
            board.nodes[index].y = newPosition.y
        }
    }
    
    public func deleteNode(id: UUID) {
        recordSnapshot()
        board.nodes.removeAll(where: { $0.id == id })
        board.edges.removeAll(where: { $0.sourceNodeId == id || $0.targetNodeId == id })
        if selectedNodeId == id {
            selectedNodeId = nil
        }
    }
    
    public func addEdge(sourceId: UUID, targetId: UUID, label: String? = nil, style: ConnectorStyle = .orthogonal) {
        guard sourceId != targetId else { return }
        recordSnapshot()
        let edge = BoardEdge(
            sourceNodeId: sourceId,
            targetNodeId: targetId,
            label: label,
            style: style
        )
        board.edges.append(edge)
    }
    
    public func deleteEdge(id: UUID) {
        recordSnapshot()
        board.edges.removeAll(where: { $0.id == id })
    }
    
    public func loadTemplate(_ type: DiagramTemplateType) {
        recordSnapshot()
        let newDiagram = LocalDiagramGenerator.generateTemplateDiagram(type: type, title: board.title)
        board.templateType = type
        board.nodes = newDiagram.nodes
        board.edges = newDiagram.edges
    }
    
    public func saveBoard() async {
        do {
            try await repository.saveDiagram(board)
        } catch {
            print("Failed to save diagram: \(error)")
        }
    }
}
