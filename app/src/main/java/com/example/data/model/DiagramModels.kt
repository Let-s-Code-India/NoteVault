package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(tableName = "diagrams")
data class DiagramEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val templateType: String = "FREEFORM", // FREEFORM, DECISION_TREE, FLOWCHART, ROADMAP, MIND_MAP, ORG_CHART, SWIMLANE
    val noteId: String? = null,
    val nodesJson: String = "[]",
    val edgesJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DiagramNode(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "RECTANGLE", // RECTANGLE, ROUNDED_CARD, DIAMOND, CIRCLE, STICKY, TEXT_ONLY, CONDITION, ACTION, OUTCOME
    val label: String = "New Node",
    val subText: String = "",
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 160f,
    val height: Float = 90f,
    val colorHex: String = "#3B82F6",
    val linkedNoteId: String? = null,
    val iconName: String? = null
)

@JsonClass(generateAdapter = true)
data class DiagramEdge(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String,
    val label: String = "",
    val lineStyle: String = "ORTHOGONAL", // STRAIGHT, CURVED, ORTHOGONAL
    val colorHex: String = "#94A3B8"
)

enum class NodeType(val displayName: String, val defaultColor: String) {
    RECTANGLE("Process Node", "#3B82F6"),
    ROUNDED_CARD("Concept Card", "#8B5CF6"),
    DIAMOND("Decision Point", "#F59E0B"),
    CIRCLE("Start / End", "#10B981"),
    STICKY("Sticky Note", "#FACC15"),
    DATABASE("Database / Storage", "#6366F1"),
    CLOUD("Cloud / API", "#0EA5E9"),
    TEXT_ONLY("Text Label", "#64748B"),
    CONDITION("If Condition", "#EC4899"),
    ACTION("Action", "#06B6D4"),
    OUTCOME("Outcome", "#84CC16")
}

enum class CanvasTemplate(val title: String, val description: String) {
    FREEFORM("Freeform Thinking", "Blank infinite canvas for structured ideas"),
    DECISION_TREE("Decision Tree", "Branching logic with conditions & outcomes"),
    FLOWCHART("System Flowchart", "Step-by-step process & workflow map"),
    ROADMAP("Project Roadmap", "Timeline milestones and phase breakdown"),
    MIND_MAP("Mind Map", "Central concept with radiating branches"),
    ORG_CHART("Organizational Hierarchy", "Structured team & reporting flow")
}
