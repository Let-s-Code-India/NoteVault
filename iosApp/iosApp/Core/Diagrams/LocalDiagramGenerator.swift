// NoteVault-iOS/Core/Diagrams/LocalDiagramGenerator.swift

import Foundation
import CoreGraphics

public struct LocalDiagramGenerator {
    
    public static func generateTemplateDiagram(type: DiagramTemplateType, title: String) -> LogicBoard {
        switch type {
        case .freeform:
            let node1 = BoardNode(label: "Main Concept", subtext: "Double tap to edit", x: 100, y: 120, width: 180, height: 90, shape: .roundedRectangle, colorHex: "#3B82F6")
            let node2 = BoardNode(label: "Key Note", subtext: "Add details here", x: 380, y: 120, width: 160, height: 80, shape: .stickyNote, colorHex: "#F59E0B", textColorHex: "#000000")
            let edge = BoardEdge(sourceNodeId: node1.id, targetNodeId: node2.id, sourceAnchor: .right, targetAnchor: .left, style: .curvedBezier)
            return LogicBoard(title: title.isEmpty ? "Freeform Board" : title, templateType: .freeform, nodes: [node1, node2], edges: [edge])
            
        case .decisionTree:
            let root = BoardNode(label: "Decision Point", subtext: "Evaluate condition", x: 260, y: 60, width: 180, height: 90, shape: .diamond, colorHex: "#6366F1")
            let yesNode = BoardNode(label: "Outcome A", subtext: "If true / accepted", x: 100, y: 220, width: 160, height: 80, shape: .roundedRectangle, colorHex: "#10B981")
            let noNode = BoardNode(label: "Outcome B", subtext: "If false / rejected", x: 420, y: 220, width: 160, height: 80, shape: .roundedRectangle, colorHex: "#EF4444")
            let edgeYes = BoardEdge(sourceNodeId: root.id, targetNodeId: yesNode.id, sourceAnchor: .left, targetAnchor: .top, label: "Yes", style: .orthogonal)
            let edgeNo = BoardEdge(sourceNodeId: root.id, targetNodeId: noNode.id, sourceAnchor: .right, targetAnchor: .top, label: "No", style: .orthogonal)
            return LogicBoard(title: title.isEmpty ? "Decision Tree" : title, templateType: .decisionTree, nodes: [root, yesNode, noNode], edges: [edgeYes, edgeNo])
            
        case .flowchart:
            let start = BoardNode(label: "Start", subtext: nil, x: 250, y: 40, width: 140, height: 60, shape: .capsule, colorHex: "#10B981")
            let process = BoardNode(label: "Process Step", subtext: "Perform calculation", x: 230, y: 150, width: 180, height: 80, shape: .rectangle, colorHex: "#3B82F6")
            let decision = BoardNode(label: "Valid?", subtext: nil, x: 245, y: 280, width: 150, height: 80, shape: .diamond, colorHex: "#F59E0B")
            let end = BoardNode(label: "End Process", subtext: nil, x: 250, y: 420, width: 140, height: 60, shape: .capsule, colorHex: "#6B7280")
            
            let e1 = BoardEdge(sourceNodeId: start.id, targetNodeId: process.id, sourceAnchor: .bottom, targetAnchor: .top, style: .straight)
            let e2 = BoardEdge(sourceNodeId: process.id, targetNodeId: decision.id, sourceAnchor: .bottom, targetAnchor: .top, style: .straight)
            let e3 = BoardEdge(sourceNodeId: decision.id, targetNodeId: end.id, sourceAnchor: .bottom, targetAnchor: .top, label: "Yes", style: .straight)
            return LogicBoard(title: title.isEmpty ? "Process Flowchart" : title, templateType: .flowchart, nodes: [start, process, decision, end], edges: [e1, e2, e3])
            
        case .roadmap:
            let phase1 = BoardNode(label: "Phase 1: Discovery", subtext: "Q1 Milestone", x: 60, y: 140, width: 160, height: 80, shape: .roundedRectangle, colorHex: "#8B5CF6")
            let phase2 = BoardNode(label: "Phase 2: Build", subtext: "Q2 Milestone", x: 270, y: 140, width: 160, height: 80, shape: .roundedRectangle, colorHex: "#3B82F6")
            let phase3 = BoardNode(label: "Phase 3: Launch", subtext: "Q3 Milestone", x: 480, y: 140, width: 160, height: 80, shape: .roundedRectangle, colorHex: "#10B981")
            
            let e1 = BoardEdge(sourceNodeId: phase1.id, targetNodeId: phase2.id, sourceAnchor: .right, targetAnchor: .left, style: .orthogonal)
            let e2 = BoardEdge(sourceNodeId: phase2.id, targetNodeId: phase3.id, sourceAnchor: .right, targetAnchor: .left, style: .orthogonal)
            return LogicBoard(title: title.isEmpty ? "Project Roadmap" : title, templateType: .roadmap, nodes: [phase1, phase2, phase3], edges: [e1, e2])
            
        case .mindMap:
            let center = BoardNode(label: "Central Theme", subtext: "Core idea", x: 260, y: 160, width: 160, height: 80, shape: .circle, colorHex: "#EC4899")
            let branch1 = BoardNode(label: "Sub-Topic A", subtext: nil, x: 60, y: 60, width: 140, height: 60, shape: .roundedRectangle, colorHex: "#8B5CF6")
            let branch2 = BoardNode(label: "Sub-Topic B", subtext: nil, x: 460, y: 60, width: 140, height: 60, shape: .roundedRectangle, colorHex: "#3B82F6")
            let branch3 = BoardNode(label: "Sub-Topic C", subtext: nil, x: 260, y: 300, width: 140, height: 60, shape: .roundedRectangle, colorHex: "#10B981")
            
            let e1 = BoardEdge(sourceNodeId: center.id, targetNodeId: branch1.id, sourceAnchor: .top, targetAnchor: .right, style: .curvedBezier)
            let e2 = BoardEdge(sourceNodeId: center.id, targetNodeId: branch2.id, sourceAnchor: .top, targetAnchor: .left, style: .curvedBezier)
            let e3 = BoardEdge(sourceNodeId: center.id, targetNodeId: branch3.id, sourceAnchor: .bottom, targetAnchor: .top, style: .curvedBezier)
            return LogicBoard(title: title.isEmpty ? "Mind Map" : title, templateType: .mindMap, nodes: [center, branch1, branch2, branch3], edges: [e1, e2, e3])
            
        case .orgChart:
            let lead = BoardNode(label: "Director / Lead", subtext: "Leadership", x: 250, y: 50, width: 180, height: 80, shape: .roundedRectangle, colorHex: "#1E293B")
            let sub1 = BoardNode(label: "Engineering", subtext: "Dev Team", x: 100, y: 190, width: 160, height: 70, shape: .roundedRectangle, colorHex: "#3B82F6")
            let sub2 = BoardNode(label: "Design & UX", subtext: "Creative Team", x: 400, y: 190, width: 160, height: 70, shape: .roundedRectangle, colorHex: "#EC4899")
            
            let e1 = BoardEdge(sourceNodeId: lead.id, targetNodeId: sub1.id, sourceAnchor: .bottom, targetAnchor: .top, style: .orthogonal)
            let e2 = BoardEdge(sourceNodeId: lead.id, targetNodeId: sub2.id, sourceAnchor: .bottom, targetAnchor: .top, style: .orthogonal)
            return LogicBoard(title: title.isEmpty ? "Org Chart" : title, templateType: .orgChart, nodes: [lead, sub1, sub2], edges: [e1, e2])
            
        case .swimlane:
            let l1 = BoardNode(label: "Lane 1: Intake", subtext: "Request received", x: 80, y: 100, width: 160, height: 75, shape: .roundedRectangle, colorHex: "#3B82F6")
            let l2 = BoardNode(label: "Lane 2: Analysis", subtext: "Review specs", x: 280, y: 200, width: 160, height: 75, shape: .roundedRectangle, colorHex: "#F59E0B")
            let l3 = BoardNode(label: "Lane 3: Delivery", subtext: "Deployment", x: 480, y: 300, width: 160, height: 75, shape: .roundedRectangle, colorHex: "#10B981")
            
            let e1 = BoardEdge(sourceNodeId: l1.id, targetNodeId: l2.id, sourceAnchor: .right, targetAnchor: .left, style: .orthogonal)
            let e2 = BoardEdge(sourceNodeId: l2.id, targetNodeId: l3.id, sourceAnchor: .right, targetAnchor: .left, style: .orthogonal)
            return LogicBoard(title: title.isEmpty ? "Swimlane Workflow" : title, templateType: .swimlane, nodes: [l1, l2, l3], edges: [e1, e2])
        }
    }
}
