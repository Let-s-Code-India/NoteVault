package com.example.ui.canvas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF as AndroidRectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.ui.components.NoteVaultTopBar
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.NoteVaultViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.*

// --- Data Models for Freehand & History ---
data class FreehandStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Offset>,
    val colorHex: String = "#38BDF8",
    val strokeWidth: Float = 4f
)

data class CanvasSnapshot(
    val nodes: List<DiagramNode>,
    val edges: List<DiagramEdge>,
    val strokes: List<FreehandStroke> = emptyList()
)

enum class CanvasTool {
    SELECT,
    CONNECT,
    PEN,
    HIGHLIGHTER,
    ERASER,
    STICKY,
    SHAPE
}

enum class CanvasGridStyle(val displayName: String) {
    DOTS("Dot Grid"),
    LINES("Graph Grid"),
    BLUEPRINT("Blueprint"),
    MINIMAL_DARK("Clean Slate")
}

// Preset color palette for nodes and strokes
val PRESET_NODE_COLORS = listOf(
    "#3B82F6", // Blue
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#EF4444", // Red
    "#F59E0B", // Amber
    "#10B981", // Emerald
    "#06B6D4", // Cyan
    "#6366F1", // Indigo
    "#84CC16", // Lime
    "#64748B"  // Slate
)

val STICKY_NOTE_COLORS = listOf(
    "#FEF08A" to "#854D0E", // Yellow
    "#FED7AA" to "#9A3412", // Peach / Orange
    "#BBF7D0" to "#166534", // Mint Green
    "#BAE6FD" to "#075985", // Sky Blue
    "#DDD6FE" to "#5B21B6", // Lavender
    "#FBCFE8" to "#9D174D"  // Rose Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogicBoardScreen(viewModel: NoteVaultViewModel) {
    val diagrams by viewModel.diagrams.collectAsState()
    val activeDiagram by viewModel.activeDiagram.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showTemplateDialog by remember { mutableStateOf(false) }

    if (activeDiagram == null) {
        // Diagram Selection / Templates List
        Scaffold(
            topBar = {
                NoteVaultTopBar(
                    title = "Logic Board",
                    syncStatus = syncStatus
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showTemplateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Canvas") },
                    modifier = Modifier.testTag("create_new_diagram_fab")
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Hero Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.AccountTree,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            "Logic Board Engine",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Infinite offline visual diagrams & roadmaps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openNewDiagram() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Blank Canvas", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { showTemplateDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Templates", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Your Boards (${diagrams.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (diagrams.isNotEmpty()) {
                            Text(
                                "Tap to edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Diagrams List or Empty State
                if (diagrams.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AccountTree,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text("No logic diagrams yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "Create flowcharts, decision trees, architecture diagrams, and mind maps with full undo/redo, smart connectors, and pen sketching.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showTemplateDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick a Starter Template")
                                }
                            }
                        }
                    }
                } else {
                    items(diagrams, key = { it.id }) { diagram ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { viewModel.openDiagram(diagram) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = when (diagram.templateType) {
                                        "DECISION_TREE" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        "ROADMAP" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        "FLOWCHART" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                        "MIND_MAP" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (diagram.templateType) {
                                                "DECISION_TREE" -> Icons.Default.AccountTree
                                                "ROADMAP" -> Icons.AutoMirrored.Filled.AltRoute
                                                "FLOWCHART" -> Icons.Default.Schema
                                                "MIND_MAP" -> Icons.Default.BubbleChart
                                                else -> Icons.Default.Hub
                                            },
                                            contentDescription = null,
                                            tint = when (diagram.templateType) {
                                                "DECISION_TREE" -> Color(0xFFD97706)
                                                "ROADMAP" -> Color(0xFF059669)
                                                "FLOWCHART" -> Color(0xFF2563EB)
                                                "MIND_MAP" -> Color(0xFF7C3AED)
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        diagram.title.ifBlank { "Untitled Diagram" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        diagram.description.ifBlank { "Interactive Visual Canvas" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteDiagram(diagram) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Interactive Full-Featured Logic Board Canvas
        InteractiveCanvasEditor(
            viewModel = viewModel,
            diagram = activeDiagram!!
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Choose Diagram Template")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CanvasTemplate.values()) { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.openNewDiagram(template.name)
                                    showTemplateDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when (template) {
                                        CanvasTemplate.DECISION_TREE -> Icons.Default.AccountTree
                                        CanvasTemplate.FLOWCHART -> Icons.Default.Schema
                                        CanvasTemplate.ROADMAP -> Icons.AutoMirrored.Filled.AltRoute
                                        CanvasTemplate.MIND_MAP -> Icons.Default.BubbleChart
                                        CanvasTemplate.ORG_CHART -> Icons.Default.CorporateFare
                                        else -> Icons.Default.Gesture
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(template.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveCanvasEditor(
    viewModel: NoteVaultViewModel,
    diagram: DiagramEntity
) {
    val context = LocalContext.current

    // Node & Edge State
    var nodes by remember(diagram.id) { mutableStateOf(viewModel.repository.parseNodesJson(diagram.nodesJson)) }
    var edges by remember(diagram.id) { mutableStateOf(viewModel.repository.parseEdgesJson(diagram.edgesJson)) }
    var freehandStrokes by remember(diagram.id) { mutableStateOf<List<FreehandStroke>>(emptyList()) }
    var currentDrawingPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Undo / Redo Stacks
    val undoStack = remember { mutableStateListOf<CanvasSnapshot>() }
    val redoStack = remember { mutableStateListOf<CanvasSnapshot>() }

    fun pushHistorySnapshot() {
        if (undoStack.size > 35) {
            undoStack.removeAt(0)
        }
        undoStack.add(CanvasSnapshot(nodes = nodes, edges = edges, strokes = freehandStrokes))
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(CanvasSnapshot(nodes = nodes, edges = edges, strokes = freehandStrokes))
            nodes = previous.nodes
            edges = previous.edges
            freehandStrokes = previous.strokes
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(CanvasSnapshot(nodes = nodes, edges = edges, strokes = freehandStrokes))
            nodes = next.nodes
            edges = next.edges
            freehandStrokes = next.strokes
        }
    }

    // Canvas View Transformations (Pan & Zoom)
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Canvas Tools & Settings
    var activeTool by remember { mutableStateOf(CanvasTool.SELECT) }
    var gridStyle by remember { mutableStateOf(CanvasGridStyle.DOTS) }
    var isSnapToGridEnabled by remember { mutableStateOf(true) }
    var activePenColor by remember { mutableStateOf("#38BDF8") }
    var activePenWidth by remember { mutableStateOf(4f) }

    // Selected / Connecting / Editing states
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var connectingFromNodeId by remember { mutableStateOf<String?>(null) }
    var selectedEdgeId by remember { mutableStateOf<String?>(null) }
    var editingNode by remember { mutableStateOf<DiagramNode?>(null) }
    var editingEdge by remember { mutableStateOf<DiagramEdge?>(null) }

    // Dialogs
    var showExportModal by remember { mutableStateOf(false) }
    var showShapePickerSheet by remember { mutableStateOf(false) }
    var showStickyPickerSheet by remember { mutableStateOf(false) }

    // Auto sync to Room
    fun persistCanvas() {
        viewModel.saveActiveDiagram(
            diagram.copy(
                nodesJson = viewModel.repository.serializeNodes(nodes),
                edgesJson = viewModel.repository.serializeEdges(edges)
            )
        )
    }

    // Snap helper
    fun snapCoord(value: Float, step: Float = 30f): Float {
        return if (isSnapToGridEnabled) (round(value / step) * step) else value
    }

    BackHandler {
        persistCanvas()
        viewModel.closeActiveDiagram()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(diagram.title.ifBlank { "Logic Canvas" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${nodes.size} nodes • ${edges.size} links", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        persistCanvas()
                        viewModel.closeActiveDiagram()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = { undo(); persistCanvas() },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { redo(); persistCanvas() },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }

                    // Auto-Layout & Tidy
                    IconButton(onClick = {
                        pushHistorySnapshot()
                        nodes = autoLayoutNodes(nodes, edges)
                        persistCanvas()
                    }) {
                        Icon(
                            Icons.Default.AutoAwesomeMosaic,
                            contentDescription = "Auto Align Diagram",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Grid Style Switcher
                    IconButton(onClick = {
                        gridStyle = when (gridStyle) {
                            CanvasGridStyle.DOTS -> CanvasGridStyle.LINES
                            CanvasGridStyle.LINES -> CanvasGridStyle.BLUEPRINT
                            CanvasGridStyle.BLUEPRINT -> CanvasGridStyle.MINIMAL_DARK
                            CanvasGridStyle.MINIMAL_DARK -> CanvasGridStyle.DOTS
                        }
                    }) {
                        Icon(
                            when (gridStyle) {
                                CanvasGridStyle.DOTS -> Icons.Default.Grain
                                CanvasGridStyle.LINES -> Icons.Default.GridOn
                                CanvasGridStyle.BLUEPRINT -> Icons.Default.Architecture
                                CanvasGridStyle.MINIMAL_DARK -> Icons.Default.LayersClear
                            },
                            contentDescription = "Grid: ${gridStyle.displayName}"
                        )
                    }

                    // Export / Share
                    IconButton(onClick = { showExportModal = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export Diagram")
                    }
                }
            )
        },
        bottomBar = {
            // Enhanced Modular Tool Dock
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    // Secondary Contextual Sub-bar (for Pen or Connect or Selected Node)
                    AnimatedVisibility(visible = activeTool == CanvasTool.PEN) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pen Color:", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("#FFFFFF", "#38BDF8", "#34D399", "#FBBF24", "#F43F5E", "#A855F7").forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (activePenColor == hex) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .clickable { activePenColor = hex }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(2f to "S", 5f to "M", 10f to "L").forEach { (width, label) ->
                                    FilterChip(
                                        selected = activePenWidth == width,
                                        onClick = { activePenWidth = width },
                                        label = { Text(label, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Primary Bottom Tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Select / Pan Tool
                        IconButton(
                            onClick = { activeTool = CanvasTool.SELECT; connectingFromNodeId = null },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (activeTool == CanvasTool.SELECT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.NearMe,
                                contentDescription = "Select & Move",
                                tint = if (activeTool == CanvasTool.SELECT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Connect Mode Tool
                        IconButton(
                            onClick = {
                                activeTool = CanvasTool.CONNECT
                                if (selectedNodeId != null) {
                                    connectingFromNodeId = selectedNodeId
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (activeTool == CanvasTool.CONNECT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingFlat,
                                contentDescription = "Link Connector Tool",
                                tint = if (activeTool == CanvasTool.CONNECT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Add Shapes
                        IconButton(onClick = { showShapePickerSheet = true }) {
                            Icon(Icons.Outlined.AddBox, contentDescription = "Add Shape Node", tint = MaterialTheme.colorScheme.primary)
                        }

                        // 4. Add Sticky Note
                        IconButton(onClick = { showStickyPickerSheet = true }) {
                            Icon(Icons.AutoMirrored.Outlined.StickyNote2, contentDescription = "Add Sticky Note", tint = Color(0xFFEAB308))
                        }

                        // 5. Freehand Sketch Pen
                        IconButton(
                            onClick = { activeTool = CanvasTool.PEN; selectedNodeId = null },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (activeTool == CanvasTool.PEN) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.Brush,
                                contentDescription = "Freehand Draw Pen",
                                tint = if (activeTool == CanvasTool.PEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 6. Highlighter Tool
                        IconButton(
                            onClick = { activeTool = CanvasTool.HIGHLIGHTER; selectedNodeId = null },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (activeTool == CanvasTool.HIGHLIGHTER) Color(0xFFFBBF24).copy(alpha = 0.25f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.Highlight,
                                contentDescription = "Neon Highlighter",
                                tint = if (activeTool == CanvasTool.HIGHLIGHTER) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 7. Eraser Tool
                        IconButton(
                            onClick = { activeTool = CanvasTool.ERASER },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (activeTool == CanvasTool.ERASER) MaterialTheme.colorScheme.errorContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.AutoFixHigh,
                                contentDescription = "Eraser Tool",
                                tint = if (activeTool == CanvasTool.ERASER) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    when (gridStyle) {
                        CanvasGridStyle.BLUEPRINT -> Color(0xFF0F2B48)
                        CanvasGridStyle.MINIMAL_DARK -> Color(0xFF090D16)
                        else -> Color(0xFF0F172A)
                    }
                )
        ) {
            // Transform Gestures Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeTool) {
                        if (activeTool == CanvasTool.SELECT || activeTool == CanvasTool.CONNECT) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.35f, 3.5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        } else if (activeTool == CanvasTool.PEN || activeTool == CanvasTool.HIGHLIGHTER) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val canvasPt = Offset(
                                        (startOffset.x - offsetX) / scale,
                                        (startOffset.y - offsetY) / scale
                                    )
                                    currentDrawingPoints = listOf(canvasPt)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val canvasPt = Offset(
                                        (change.position.x - offsetX) / scale,
                                        (change.position.y - offsetY) / scale
                                    )
                                    currentDrawingPoints = currentDrawingPoints + canvasPt
                                },
                                onDragEnd = {
                                    if (currentDrawingPoints.size > 1) {
                                        pushHistorySnapshot()
                                        val strokeColor = if (activeTool == CanvasTool.HIGHLIGHTER) "#66FBBF24" else activePenColor
                                        val strokeW = if (activeTool == CanvasTool.HIGHLIGHTER) 24f else activePenWidth
                                        freehandStrokes = freehandStrokes + FreehandStroke(
                                            points = currentDrawingPoints,
                                            colorHex = strokeColor,
                                            strokeWidth = strokeW
                                        )
                                        currentDrawingPoints = emptyList()
                                        persistCanvas()
                                    }
                                }
                            )
                        }
                    }
            ) {
                // Background Grid and Edges/Connectors Canvas Layer
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                ) {
                    // 1. Grid Background
                    val gridStep = 40f
                    val canvasWidth = size.width / scale + 1000f
                    val canvasHeight = size.height / scale + 1000f

                    when (gridStyle) {
                        CanvasGridStyle.DOTS -> {
                            val dotColor = Color.White.copy(alpha = 0.09f)
                            for (x in -500..canvasWidth.toInt() step gridStep.toInt()) {
                                for (y in -500..canvasHeight.toInt() step gridStep.toInt()) {
                                    drawCircle(dotColor, radius = 1.6f, center = Offset(x.toFloat(), y.toFloat()))
                                }
                            }
                        }
                        CanvasGridStyle.LINES -> {
                            val lineColor = Color.White.copy(alpha = 0.05f)
                            for (x in -500..canvasWidth.toInt() step gridStep.toInt()) {
                                drawLine(lineColor, start = Offset(x.toFloat(), -500f), end = Offset(x.toFloat(), canvasHeight), strokeWidth = 1f)
                            }
                            for (y in -500..canvasHeight.toInt() step gridStep.toInt()) {
                                drawLine(lineColor, start = Offset(-500f, y.toFloat()), end = Offset(canvasWidth, y.toFloat()), strokeWidth = 1f)
                            }
                        }
                        CanvasGridStyle.BLUEPRINT -> {
                            val majorLine = Color(0xFF38BDF8).copy(alpha = 0.12f)
                            for (x in -500..canvasWidth.toInt() step (gridStep * 2).toInt()) {
                                drawLine(majorLine, start = Offset(x.toFloat(), -500f), end = Offset(x.toFloat(), canvasHeight), strokeWidth = 1.5f)
                            }
                            for (y in -500..canvasHeight.toInt() step (gridStep * 2).toInt()) {
                                drawLine(majorLine, start = Offset(-500f, y.toFloat()), end = Offset(canvasWidth, y.toFloat()), strokeWidth = 1.5f)
                            }
                        }
                        CanvasGridStyle.MINIMAL_DARK -> {}
                    }

                    // 2. Freehand Saved Strokes
                    freehandStrokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val strokeColor = try {
                                Color(android.graphics.Color.parseColor(stroke.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF38BDF8)
                            }
                            val path = Path().apply {
                                moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // 3. Current Live Drawing Stroke
                    if (currentDrawingPoints.size > 1) {
                        val strokeColor = try {
                            Color(android.graphics.Color.parseColor(activePenColor))
                        } catch (e: Exception) {
                            Color.White
                        }
                        val livePath = Path().apply {
                            moveTo(currentDrawingPoints[0].x, currentDrawingPoints[0].y)
                            for (i in 1 until currentDrawingPoints.size) {
                                lineTo(currentDrawingPoints[i].x, currentDrawingPoints[i].y)
                            }
                        }
                        drawPath(
                            path = livePath,
                            color = strokeColor,
                            style = Stroke(
                                width = activePenWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 4. Directed Connectors / Edges
                    edges.forEach { edge ->
                        val fromNode = nodes.find { it.id == edge.fromNodeId }
                        val toNode = nodes.find { it.id == edge.toNodeId }

                        if (fromNode != null && toNode != null) {
                            val startCenter = Offset(fromNode.x + fromNode.width / 2f, fromNode.y + fromNode.height / 2f)
                            val endCenter = Offset(toNode.x + toNode.width / 2f, toNode.y + toNode.height / 2f)

                            // Calculate intersection with target node boundary
                            val angle = atan2(endCenter.y - startCenter.y, endCenter.x - startCenter.x)
                            val targetBorderOffset = calculateBorderOffset(toNode, angle)
                            val endPt = Offset(endCenter.x - targetBorderOffset.x, endCenter.y - targetBorderOffset.y)
                            val startBorderOffset = calculateBorderOffset(fromNode, angle + Math.PI.toFloat())
                            val startPt = Offset(startCenter.x - startBorderOffset.x, startCenter.y - startBorderOffset.y)

                            val isEdgeSelected = selectedEdgeId == edge.id
                            val edgeColor = if (isEdgeSelected) Color(0xFFFBBF24) else try {
                                Color(android.graphics.Color.parseColor(edge.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF94A3B8)
                            }

                            when (edge.lineStyle) {
                                "CURVED" -> {
                                    val midX = (startPt.x + endPt.x) / 2f
                                    val midY = (startPt.y + endPt.y) / 2f
                                    val perpX = -(endPt.y - startPt.y) * 0.25f
                                    val perpY = (endPt.x - startPt.x) * 0.25f
                                    val ctrl = Offset(midX + perpX, midY + perpY)

                                    val path = Path().apply {
                                        moveTo(startPt.x, startPt.y)
                                        quadraticTo(ctrl.x, ctrl.y, endPt.x, endPt.y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = edgeColor,
                                        style = Stroke(
                                            width = if (isEdgeSelected) 4f else 2.5f,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                    drawArrowHead(endPt, angle, edgeColor)
                                }
                                "ORTHOGONAL" -> {
                                    val midX = (startPt.x + endPt.x) / 2f
                                    val path = Path().apply {
                                        moveTo(startPt.x, startPt.y)
                                        lineTo(midX, startPt.y)
                                        lineTo(midX, endPt.y)
                                        lineTo(endPt.x, endPt.y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = edgeColor,
                                        style = Stroke(
                                            width = if (isEdgeSelected) 4f else 2.5f,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                    val finalAngle = if (endPt.x > midX) 0f else Math.PI.toFloat()
                                    drawArrowHead(endPt, finalAngle, edgeColor)
                                }
                                else -> {
                                    // Straight line
                                    drawLine(
                                        color = edgeColor,
                                        start = startPt,
                                        end = endPt,
                                        strokeWidth = if (isEdgeSelected) 4f else 2.5f,
                                        cap = StrokeCap.Round
                                    )
                                    drawArrowHead(endPt, angle, edgeColor)
                                }
                            }
                        }
                    }
                }

                // Interactive Nodes Canvas Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                ) {
                    nodes.forEach { node ->
                        val isSelected = selectedNodeId == node.id
                        val isConnectingSource = connectingFromNodeId == node.id

                        val nodeColor = try {
                            Color(android.graphics.Color.parseColor(node.colorHex))
                        } catch (e: Exception) {
                            Color(0xFF3B82F6)
                        }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
                                .size(node.width.dp, node.height.dp)
                                .shadow(
                                    elevation = if (isSelected) 10.dp else 4.dp,
                                    shape = getNodeShape(node.type)
                                )
                                .clip(getNodeShape(node.type))
                                .then(
                                    if (node.type == "STICKY") {
                                        Modifier.background(nodeColor)
                                    } else {
                                        Modifier.background(
                                            Brush.verticalGradient(
                                                listOf(nodeColor.copy(alpha = 0.95f), nodeColor.copy(alpha = 0.75f))
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isConnectingSource) 3.dp else if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isConnectingSource) Color(0xFF10B981) else if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = getNodeShape(node.type)
                                )
                                .clickable {
                                    if (activeTool == CanvasTool.CONNECT) {
                                        if (connectingFromNodeId == null) {
                                            connectingFromNodeId = node.id
                                        } else if (connectingFromNodeId != node.id) {
                                            // Create edge!
                                            pushHistorySnapshot()
                                            val newEdge = DiagramEdge(
                                                id = UUID.randomUUID().toString(),
                                                fromNodeId = connectingFromNodeId!!,
                                                toNodeId = node.id,
                                                lineStyle = "ORTHOGONAL",
                                                colorHex = "#94A3B8"
                                            )
                                            edges = edges + newEdge
                                            connectingFromNodeId = null
                                            persistCanvas()
                                        }
                                    } else if (activeTool == CanvasTool.ERASER) {
                                        pushHistorySnapshot()
                                        nodes = nodes.filter { it.id != node.id }
                                        edges = edges.filter { it.fromNodeId != node.id && it.toNodeId != node.id }
                                        selectedNodeId = null
                                        persistCanvas()
                                    } else {
                                        selectedNodeId = if (selectedNodeId == node.id) null else node.id
                                    }
                                }
                                .pointerInput(node.id, activeTool) {
                                    if (activeTool == CanvasTool.SELECT) {
                                        detectDragGestures(
                                            onDragStart = {
                                                selectedNodeId = node.id
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                nodes = nodes.map { n ->
                                                    if (n.id == node.id) {
                                                        val rawX = n.x + dragAmount.x / scale
                                                        val rawY = n.y + dragAmount.y / scale
                                                        n.copy(x = rawX, y = rawY)
                                                    } else n
                                                }
                                            },
                                            onDragEnd = {
                                                pushHistorySnapshot()
                                                nodes = nodes.map { n ->
                                                    if (n.id == node.id) {
                                                        n.copy(
                                                            x = snapCoord(n.x),
                                                            y = snapCoord(n.y)
                                                        )
                                                    } else n
                                                }
                                                persistCanvas()
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Node content layout
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                // Node Icon / Shape Indicator
                                if (node.type == "DATABASE") {
                                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                } else if (node.type == "CLOUD") {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                } else if (node.type == "DIAMOND") {
                                    Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                Text(
                                    text = node.label.ifBlank { "Node" },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (node.type == "STICKY") Color(0xFF1F2937) else Color.White
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (node.subText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = node.subText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            color = if (node.type == "STICKY") Color(0xFF4B5563) else Color.White.copy(alpha = 0.8f)
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Quick Edit Trigger on selection
                            if (isSelected) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clickable { editingNode = node }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Node", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Floating HUD (Zoom & Snap to Grid controls)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.2f).coerceAtLeast(0.4f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        "${(scale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { scale = (scale + 0.2f).coerceAtMost(3.0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }

                    VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))

                    IconButton(
                        onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "Center View", modifier = Modifier.size(16.dp))
                    }

                    FilterChip(
                        selected = isSnapToGridEnabled,
                        onClick = { isSnapToGridEnabled = !isSnapToGridEnabled },
                        label = { Text("Snap", fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            // Node Action Context Menu (Floats over bottom when a node is selected)
            if (selectedNodeId != null) {
                val selectedNode = nodes.find { it.id == selectedNodeId }
                if (selectedNode != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                selectedNode.label.take(12),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )

                            VerticalDivider(modifier = Modifier.height(20.dp))

                            IconButton(
                                onClick = { editingNode = selectedNode },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Text", modifier = Modifier.size(18.dp))
                            }

                            // Quick Branch / Connect to New Child Node
                            IconButton(
                                onClick = {
                                    pushHistorySnapshot()
                                    val newId = UUID.randomUUID().toString()
                                    val childNode = DiagramNode(
                                        id = newId,
                                        label = "Sub-Concept",
                                        x = selectedNode.x + selectedNode.width + 70f,
                                        y = selectedNode.y,
                                        colorHex = selectedNode.colorHex,
                                        type = selectedNode.type,
                                        width = selectedNode.width,
                                        height = selectedNode.height
                                    )
                                    val newEdge = DiagramEdge(
                                        id = UUID.randomUUID().toString(),
                                        fromNodeId = selectedNode.id,
                                        toNodeId = newId,
                                        label = "",
                                        colorHex = selectedNode.colorHex
                                    )
                                    nodes = nodes + childNode
                                    edges = edges + newEdge
                                    selectedNodeId = newId
                                    persistCanvas()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ForkRight, contentDescription = "Branch Out", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }

                            // Duplicate Node
                            IconButton(
                                onClick = {
                                    pushHistorySnapshot()
                                    val cloned = selectedNode.copy(
                                        id = UUID.randomUUID().toString(),
                                        label = "${selectedNode.label} (Copy)",
                                        x = selectedNode.x + 40f,
                                        y = selectedNode.y + 40f
                                    )
                                    nodes = nodes + cloned
                                    selectedNodeId = cloned.id
                                    persistCanvas()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                            }

                            // Color Swatch Menu
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PRESET_NODE_COLORS.take(4).forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .clickable {
                                                pushHistorySnapshot()
                                                nodes = nodes.map { if (it.id == selectedNode.id) it.copy(colorHex = hex) else it }
                                                persistCanvas()
                                            }
                                    )
                                }
                            }

                            VerticalDivider(modifier = Modifier.height(20.dp))

                            // Delete Node
                            IconButton(
                                onClick = {
                                    pushHistorySnapshot()
                                    nodes = nodes.filter { it.id != selectedNode.id }
                                    edges = edges.filter { it.fromNodeId != selectedNode.id && it.toNodeId != selectedNode.id }
                                    selectedNodeId = null
                                    persistCanvas()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Node", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to Pick New Shapes
    if (showShapePickerSheet) {
        AlertDialog(
            onDismissRequest = { showShapePickerSheet = false },
            title = { Text("Add Shape to Board") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val shapeOptions = listOf(
                        Triple("RECTANGLE", "Process Step", "#3B82F6"),
                        Triple("DIAMOND", "Decision Point", "#F59E0B"),
                        Triple("CIRCLE", "Start / End State", "#10B981"),
                        Triple("DATABASE", "Database Storage", "#6366F1"),
                        Triple("CLOUD", "Cloud / Remote API", "#0EA5E9"),
                        Triple("ROUNDED_CARD", "Concept Card", "#8B5CF6")
                    )

                    shapeOptions.forEach { (type, label, colorHex) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pushHistorySnapshot()
                                    val spawnX = snapCoord((-offsetX + 180f) / scale)
                                    val spawnY = snapCoord((-offsetY + 260f) / scale)
                                    val newNode = DiagramNode(
                                        id = UUID.randomUUID().toString(),
                                        type = type,
                                        label = label,
                                        x = spawnX,
                                        y = spawnY,
                                        colorHex = colorHex,
                                        width = if (type == "CIRCLE") 120f else 160f,
                                        height = if (type == "CIRCLE") 120f else 90f
                                    )
                                    nodes = nodes + newNode
                                    selectedNodeId = newNode.id
                                    showShapePickerSheet = false
                                    persistCanvas()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                )
                                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShapePickerSheet = false }) { Text("Cancel") }
            }
        )
    }

    // Modal Sheet to Pick Sticky Note Color
    if (showStickyPickerSheet) {
        AlertDialog(
            onDismissRequest = { showStickyPickerSheet = false },
            title = { Text("Add Sticky Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select sticky color:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        STICKY_NOTE_COLORS.forEach { (bgHex, textHex) ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(bgHex)))
                                    .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        pushHistorySnapshot()
                                        val spawnX = snapCoord((-offsetX + 180f) / scale)
                                        val spawnY = snapCoord((-offsetY + 260f) / scale)
                                        val newNode = DiagramNode(
                                            id = UUID.randomUUID().toString(),
                                            type = "STICKY",
                                            label = "New Idea",
                                            subText = "Note details...",
                                            x = spawnX,
                                            y = spawnY,
                                            width = 150f,
                                            height = 150f,
                                            colorHex = bgHex
                                        )
                                        nodes = nodes + newNode
                                        selectedNodeId = newNode.id
                                        showStickyPickerSheet = false
                                        persistCanvas()
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStickyPickerSheet = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Node Details Dialog
    if (editingNode != null) {
        var label by remember { mutableStateOf(editingNode!!.label) }
        var subText by remember { mutableStateOf(editingNode!!.subText) }
        var selectedColor by remember { mutableStateOf(editingNode!!.colorHex) }
        var selectedType by remember { mutableStateOf(editingNode!!.type) }

        AlertDialog(
            onDismissRequest = { editingNode = null },
            title = { Text("Edit Node Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Title / Main Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subText,
                        onValueChange = { subText = it },
                        label = { Text("Subtitle / Details") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Color Palette:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_NODE_COLORS) { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selectedColor == hex) 3.dp else 1.dp,
                                        color = if (selectedColor == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    pushHistorySnapshot()
                    nodes = nodes.map {
                        if (it.id == editingNode!!.id) {
                            it.copy(label = label, subText = subText, colorHex = selectedColor, type = selectedType)
                        } else it
                    }
                    persistCanvas()
                    editingNode = null
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNode = null }) { Text("Cancel") }
            }
        )
    }

    // Export Dialog (Mermaid Markdown, PNG Bitmap Image, & JSON)
    if (showExportModal) {
        val jsonExport = remember { viewModel.repository.serializeNodes(nodes) }
        val mermaidCode = remember(nodes, edges) { generateMermaidGraph(nodes, edges) }

        AlertDialog(
            onDismissRequest = { showExportModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.IosShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Export Diagram")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Option 1: Share Image
                    Button(
                        onClick = {
                            viewModel.exportDiagramPng(context, diagram)
                            showExportModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Share as PNG Image")
                    }

                    // Option 2: Share Scalable Vector SVG
                    OutlinedButton(
                        onClick = {
                            viewModel.exportDiagramSvg(context, diagram)
                            showExportModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Scalable Vector (.svg)")
                    }

                    // Option 3: Share Mermaid Diagram File
                    OutlinedButton(
                        onClick = {
                            viewModel.exportDiagramMermaid(context, diagram)
                            showExportModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Mermaid Diagram (.mmd)")
                    }

                    // Option 4: JSON Re-importable Format
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, jsonExport)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Diagram JSON"))
                            showExportModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Raw JSON")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportModal = false }) { Text("Close") }
            }
        )
    }
}

// --- Helper Functions for Shapes, Arrows & Export ---

fun getNodeShape(type: String): androidx.compose.ui.graphics.Shape {
    return when (type) {
        "DIAMOND" -> RoundedCornerShape(16.dp)
        "CIRCLE" -> CircleShape
        "ROUNDED_CARD" -> RoundedCornerShape(20.dp)
        "STICKY" -> RoundedCornerShape(6.dp)
        "DATABASE" -> RoundedCornerShape(12.dp)
        "CLOUD" -> RoundedCornerShape(24.dp)
        else -> RoundedCornerShape(10.dp)
    }
}

fun calculateBorderOffset(node: DiagramNode, angle: Float): Offset {
    val halfW = node.width / 2f
    val halfH = node.height / 2f
    val cosA = cos(angle)
    val sinA = sin(angle)

    val factor = min(
        abs(if (cosA != 0f) halfW / cosA else Float.MAX_VALUE),
        abs(if (sinA != 0f) halfH / sinA else Float.MAX_VALUE)
    )
    return Offset(cosA * factor, sinA * factor)
}

fun DrawScope.drawArrowHead(tip: Offset, angle: Float, color: Color) {
    val arrowLen = 14f
    val arrowAngle = Math.PI.toFloat() / 6f

    val p1 = Offset(
        tip.x - arrowLen * cos(angle - arrowAngle),
        tip.y - arrowLen * sin(angle - arrowAngle)
    )
    val p2 = Offset(
        tip.x - arrowLen * cos(angle + arrowAngle),
        tip.y - arrowLen * sin(angle + arrowAngle)
    )

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color)
}

fun generateMermaidGraph(nodes: List<DiagramNode>, edges: List<DiagramEdge>): String {
    val sb = StringBuilder()
    sb.append("```mermaid\ngraph TD\n")
    nodes.forEach { node ->
        val safeId = node.id.replace("-", "").take(8)
        val safeLabel = node.label.replace("\"", "'")
        when (node.type) {
            "DIAMOND" -> sb.append("  $safeId{\"$safeLabel\"}\n")
            "CIRCLE" -> sb.append("  $safeId((\"$safeLabel\"))\n")
            "DATABASE" -> sb.append("  $safeId[(\"$safeLabel\")]\n")
            else -> sb.append("  $safeId[\"$safeLabel\"]\n")
        }
    }
    edges.forEach { edge ->
        val fromSafe = edge.fromNodeId.replace("-", "").take(8)
        val toSafe = edge.toNodeId.replace("-", "").take(8)
        val labelPart = if (edge.label.isNotBlank()) "|${edge.label}|" else ""
        sb.append("  $fromSafe -->$labelPart $toSafe\n")
    }
    sb.append("```")
    return sb.toString()
}

fun generateDiagramBitmap(nodes: List<DiagramNode>, edges: List<DiagramEdge>, strokes: List<FreehandStroke>): Bitmap {
    val width = 1200
    val height = 1600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Fill dark background
    canvas.drawColor(android.graphics.Color.parseColor("#0F172A"))

    val paint = AndroidPaint().apply {
        isAntiAlias = true
    }

    // Draw grid dots
    paint.color = android.graphics.Color.parseColor("#20FFFFFF")
    for (x in 0..width step 40) {
        for (y in 0..height step 40) {
            canvas.drawCircle(x.toFloat(), y.toFloat(), 1.5f, paint)
        }
    }

    // Draw edges
    edges.forEach { edge ->
        val from = nodes.find { it.id == edge.fromNodeId }
        val to = nodes.find { it.id == edge.toNodeId }
        if (from != null && to != null) {
            val fromX = from.x + from.width / 2f
            val fromY = from.y + from.height / 2f
            val toX = to.x + to.width / 2f
            val toY = to.y + to.height / 2f

            paint.color = try {
                android.graphics.Color.parseColor(edge.colorHex)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#94A3B8")
            }
            paint.strokeWidth = 3f
            paint.style = AndroidPaint.Style.STROKE
            canvas.drawLine(fromX, fromY, toX, toY, paint)
        }
    }

    // Draw nodes
    nodes.forEach { node ->
        val rect = AndroidRectF(node.x, node.y, node.x + node.width, node.y + node.height)
        paint.style = AndroidPaint.Style.FILL
        paint.color = try {
            android.graphics.Color.parseColor(node.colorHex)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#3B82F6")
        }
        canvas.drawRoundRect(rect, 16f, 16f, paint)

        // Text
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 14f * 2.5f
        paint.textAlign = AndroidPaint.Align.CENTER
        canvas.drawText(node.label, node.x + node.width / 2f, node.y + node.height / 2f + 10f, paint)
    }

    return bitmap
}

fun autoLayoutNodes(currentNodes: List<DiagramNode>, currentEdges: List<DiagramEdge>): List<DiagramNode> {
    if (currentNodes.isEmpty()) return currentNodes
    val incomingCount = currentNodes.associate { it.id to 0 }.toMutableMap()
    for (edge in currentEdges) {
        if (incomingCount.containsKey(edge.toNodeId)) {
            incomingCount[edge.toNodeId] = (incomingCount[edge.toNodeId] ?: 0) + 1
        }
    }

    val rootNodes = currentNodes.filter { (incomingCount[it.id] ?: 0) == 0 }.ifEmpty { listOf(currentNodes.first()) }
    val levels = mutableMapOf<String, Int>()
    val queue = ArrayDeque<Pair<String, Int>>()
    rootNodes.forEach {
        levels[it.id] = 0
        queue.add(it.id to 0)
    }

    val visited = mutableSetOf<String>()
    while (queue.isNotEmpty()) {
        val (nodeId, level) = queue.removeFirst()
        if (visited.add(nodeId)) {
            val outgoing = currentEdges.filter { it.fromNodeId == nodeId }
            for (edge in outgoing) {
                val nextNode = edge.toNodeId
                val nextLevel = level + 1
                if ((levels[nextNode] ?: 0) < nextLevel) {
                    levels[nextNode] = nextLevel
                }
                queue.add(nextNode to nextLevel)
            }
        }
    }

    val maxLevel = levels.values.maxOrNull() ?: 0
    currentNodes.forEach { node ->
        if (!levels.containsKey(node.id)) {
            levels[node.id] = maxLevel + 1
        }
    }

    val groupedByLevel = currentNodes.groupBy { levels[it.id] ?: 0 }
    val result = mutableListOf<DiagramNode>()

    val startX = 80f
    val startY = 120f
    val levelSpacingX = 260f
    val nodeSpacingY = 150f

    groupedByLevel.keys.sorted().forEach { lvl ->
        val nodesAtLvl = groupedByLevel[lvl] ?: emptyList()
        nodesAtLvl.forEachIndexed { index, node ->
            val newX = startX + lvl * levelSpacingX
            val newY = startY + index * nodeSpacingY
            result.add(node.copy(x = newX, y = newY))
        }
    }
    return result
}
