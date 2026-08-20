// NoteVault-iOS/Views/LogicBoard/LogicBoardCanvasView.swift

import SwiftUI

public struct LogicBoardCanvasView: View {
    @StateObject private var viewModel: LogicBoardViewModel
    @State private var canvasScale: CGFloat = 1.0
    @State private var canvasOffset: CGSize = .zero
    
    public init(board: LogicBoard? = nil) {
        _viewModel = StateObject(wrappedValue: LogicBoardViewModel(board: board))
    }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                // Background grid
                CanvasGridBackground()
                    .ignoresSafeArea()
                
                // Infinite Canvas Content Area
                GeometryReader { _ in
                    ZStack {
                        // Connectors / Edges Layer
                        ForEach(viewModel.board.edges) { edge in
                            let src = viewModel.board.nodes.first(where: { $0.id == edge.sourceNodeId })
                            let dst = viewModel.board.nodes.first(where: { $0.id == edge.targetNodeId })
                            DiagramConnectorView(edge: edge, sourceNode: src, targetNode: dst)
                        }
                        
                        // Nodes Layer
                        ForEach(viewModel.board.nodes) { node in
                            DiagramNodeView(
                                node: node,
                                isSelected: viewModel.selectedNodeId == node.id,
                                onPositionChanged: { newPos in
                                    viewModel.updateNodePosition(id: node.id, newPosition: newPos)
                                }
                            )
                            .onTapGesture {
                                if viewModel.activeTool == .connect {
                                    if let source = viewModel.connectingSourceNodeId, source != node.id {
                                        viewModel.addEdge(sourceId: source, targetId: node.id)
                                        viewModel.connectingSourceNodeId = nil
                                        viewModel.activeTool = .select
                                    } else {
                                        viewModel.connectingSourceNodeId = node.id
                                    }
                                } else if viewModel.activeTool == .delete {
                                    viewModel.deleteNode(id: node.id)
                                } else {
                                    viewModel.selectedNodeId = node.id
                                }
                            }
                        }
                    }
                    .scaleEffect(canvasScale)
                    .offset(canvasOffset)
                }
                .gesture(
                    DragGesture()
                        .onChanged { val in
                            canvasOffset = CGSize(
                                width: canvasOffset.width + val.translation.width * 0.1,
                                height: canvasOffset.height + val.translation.height * 0.1
                            )
                        }
                )
                
                // Floating Toolbar
                VStack {
                    Spacer()
                    floatingControlsBar
                        .padding(.bottom, 24)
                }
            }
            .navigationTitle(viewModel.board.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.undo()
                    } label: {
                        Image(systemName: "arrow.uturn.backward")
                    }
                    
                    Button {
                        viewModel.redo()
                    } label: {
                        Image(systemName: "arrow.uturn.forward")
                    }
                    
                    Button {
                        viewModel.isPresentingTemplatePicker = true
                    } label: {
                        Label("Apply Template", systemImage: "square.grid.2x2")
                    }
                }
            }
            .sheet(isPresented: $viewModel.isPresentingTemplatePicker) {
                TemplatePickerSheet { selectedType in
                    viewModel.loadTemplate(selectedType)
                }
            }
        }
    }
    
    private var floatingControlsBar: some View {
        HStack(spacing: 16) {
            Button {
                viewModel.addNode(label: "New Idea", at: CGPoint(x: 200, y: 200))
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
            }
            
            Divider().frame(height: 20)
            
            Button {
                viewModel.activeTool = .select
            } label: {
                Image(systemName: "hand.point.up.left.fill")
                    .foregroundColor(viewModel.activeTool == .select ? .accentColor : .secondary)
            }
            
            Button {
                viewModel.activeTool = .connect
            } label: {
                Image(systemName: "arrowshape.turn.up.right.fill")
                    .foregroundColor(viewModel.activeTool == .connect ? .accentColor : .secondary)
            }
            
            Button {
                viewModel.activeTool = .delete
            } label: {
                Image(systemName: "trash.fill")
                    .foregroundColor(viewModel.activeTool == .delete ? .red : .secondary)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.15), radius: 10, x: 0, y: 4)
    }
}

public struct CanvasGridBackground: View {
    public var body: some View {
        GeometryReader { geo in
            Path { path in
                let spacing: CGFloat = 30
                let cols = Int(geo.size.width / spacing) + 1
                let rows = Int(geo.size.height / spacing) + 1
                
                for i in 0..<cols {
                    for j in 0..<rows {
                        let x = CGFloat(i) * spacing
                        let y = CGFloat(j) * spacing
                        path.addEllipse(in: CGRect(x: x, y: y, width: 2, height: 2))
                    }
                }
            }
            .fill(Color.primary.opacity(0.1))
        }
    }
}
