// NoteVault-iOS/Models/Domain/DiagramTemplate.swift

import Foundation

public enum DiagramTemplateType: String, CaseIterable, Codable, Sendable {
    case freeform = "freeform"
    case decisionTree = "decision_tree"
    case flowchart = "flowchart"
    case roadmap = "roadmap"
    case mindMap = "mind_map"
    case orgChart = "org_chart"
    case swimlane = "swimlane"
    
    public var displayName: String {
        switch self {
        case .freeform: return "Freeform Canvas"
        case .decisionTree: return "Decision Tree"
        case .flowchart: return "Flowchart"
        case .roadmap: return "Product Roadmap"
        case .mindMap: return "Mind Map"
        case .orgChart: return "Organization Chart"
        case .swimlane: return "Swimlane Diagram"
        }
    }
    
    public var iconName: String {
        switch self {
        case .freeform: return "hand.draw"
        case .decisionTree: return "arrow.triangle.branch"
        case .flowchart: return "rectangle.2.swap"
        case .roadmap: return "timeline.selection"
        case .mindMap: return "brain.head.profile"
        case .orgChart: return "person.3.sequence.fill"
        case .swimlane: return "tablecells"
        }
    }
    
    public var descriptionText: String {
        switch self {
        case .freeform: return "Blank boundless canvas for sketches and custom thoughts."
        case .decisionTree: return "Branching outcomes with conditional yes/no logic nodes."
        case .flowchart: return "Sequential processes, start/end terminators, and decision diamonds."
        case .roadmap: return "Milestone phases ordered chronologically."
        case .mindMap: return "Central root concept expanding radially into sub-branches."
        case .orgChart: return "Hierarchical reporting structures and team chains."
        case .swimlane: return "Multi-lane categorical workflow mapping."
        }
    }
}
