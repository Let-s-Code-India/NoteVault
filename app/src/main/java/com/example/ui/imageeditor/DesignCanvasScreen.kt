package com.example.ui.imageeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

sealed class CanvaElement {
    abstract val id: String
    abstract var x: Float
    abstract var y: Float
    abstract var width: Float
    abstract var height: Float

    fun clone(): CanvaElement = when (this) {
        is ImageItem -> this.copy()
        is TextItem -> this.copy()
        is StickyItem -> this.copy()
    }

    data class ImageItem(
        override val id: String = UUID.randomUUID().toString(),
        override var x: Float,
        override var y: Float,
        override var width: Float = 200f,
        override var height: Float = 200f,
        val bitmap: Bitmap
    ) : CanvaElement()

    data class TextItem(
        override val id: String = UUID.randomUUID().toString(),
        override var x: Float,
        override var y: Float,
        override var width: Float = 220f,
        override var height: Float = 80f,
        var text: String,
        var colorHex: String = "#1E293B",
        var fontSize: Float = 20f
    ) : CanvaElement()

    data class StickyItem(
        override val id: String = UUID.randomUUID().toString(),
        override var x: Float,
        override var y: Float,
        override var width: Float = 180f,
        override var height: Float = 180f,
        var text: String,
        var bgHex: String = "#FEF08A"
    ) : CanvaElement()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignCanvasScreen(
    onDismiss: () -> Unit,
    onSaveComposition: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val elements = remember { mutableStateListOf<CanvaElement>() }
    val undoStack = remember { mutableStateListOf<List<CanvaElement>>() }
    val redoStack = remember { mutableStateListOf<List<CanvaElement>>() }

    fun pushUndoSnapshot() {
        if (undoStack.size > 25) undoStack.removeAt(0)
        undoStack.add(elements.map { it.clone() })
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(elements.map { it.clone() })
            elements.clear()
            elements.addAll(prev.map { it.clone() })
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(elements.map { it.clone() })
            elements.clear()
            elements.addAll(next.map { it.clone() })
        }
    }

    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var showAddStickyDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var stickyInput by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = ImageProcessingUtils.loadBitmapFromUri(context, it)
            if (bitmap != null) {
                pushUndoSnapshot()
                elements.add(
                    CanvaElement.ImageItem(
                        x = 100f + elements.size * 20f,
                        y = 100f + elements.size * 20f,
                        bitmap = bitmap
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Canva Studio", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { undo() },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Canvas",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = { redo() },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo Canvas",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    Button(
                        onClick = {
                            if (elements.isEmpty()) return@Button
                            // Render composition to Bitmap
                            val compWidth = 1080
                            val compHeight = 1080
                            val bitmap = Bitmap.createBitmap(compWidth, compHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(AndroidColor.WHITE)

                            val paintText = Paint().apply { isAntiAlias = true }
                            val paintBg = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

                            for (el in elements) {
                                when (el) {
                                    is CanvaElement.ImageItem -> {
                                        val destRect = RectF(el.x, el.y, el.x + el.width, el.y + el.height)
                                        canvas.drawBitmap(el.bitmap, null, destRect, paintText)
                                    }
                                    is CanvaElement.TextItem -> {
                                        paintText.color = AndroidColor.parseColor(el.colorHex)
                                        paintText.textSize = el.fontSize * 1.5f
                                        paintText.isFakeBoldText = true
                                        canvas.drawText(el.text, el.x, el.y + el.height / 2f, paintText)
                                    }
                                    is CanvaElement.StickyItem -> {
                                        paintBg.color = AndroidColor.parseColor(el.bgHex)
                                        val rect = RectF(el.x, el.y, el.x + el.width, el.y + el.height)
                                        canvas.drawRoundRect(rect, 16f, 16f, paintBg)
                                        paintText.color = AndroidColor.BLACK
                                        paintText.textSize = 24f
                                        canvas.drawText(el.text, el.x + 20f, el.y + 60f, paintText)
                                    }
                                }
                            }
                            onSaveComposition(bitmap)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Note")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image", tint = MaterialTheme.colorScheme.primary)
                            Text("Image", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { showAddTextDialog = true }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TextFields, contentDescription = "Add Text", tint = MaterialTheme.colorScheme.primary)
                            Text("Text", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { showAddStickyDialog = true }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Add Sticky", tint = MaterialTheme.colorScheme.primary)
                            Text("Sticky", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (selectedElementId != null) {
                        IconButton(onClick = {
                            pushUndoSnapshot()
                            elements.removeAll { it.id == selectedElementId }
                            selectedElementId = null
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                                Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
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
                .background(Color(0xFFF1F5F9))
        ) {
            // Interactive Canvas Surface
            elements.forEach { element ->
                val isSelected = element.id == selectedElementId
                Box(
                    modifier = Modifier
                        .offset(x = element.x.dp, y = element.y.dp)
                        .size(width = element.width.dp, height = element.height.dp)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .pointerInput(element.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                element.x += dragAmount.x
                                element.y += dragAmount.y
                                selectedElementId = element.id
                            }
                        }
                ) {
                    when (element) {
                        is CanvaElement.ImageItem -> {
                            androidx.compose.foundation.Image(
                                bitmap = element.bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                        }
                        is CanvaElement.TextItem -> {
                            Text(
                                text = element.text,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = element.fontSize.sp,
                                    color = Color(android.graphics.Color.parseColor(element.colorHex))
                                ),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        is CanvaElement.StickyItem -> {
                            Surface(
                                color = Color(android.graphics.Color.parseColor(element.bgHex)),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = element.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Add Text Overlay") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Text content") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (textInput.isNotBlank()) {
                        pushUndoSnapshot()
                        elements.add(
                            CanvaElement.TextItem(
                                x = 120f,
                                y = 120f,
                                text = textInput
                            )
                        )
                        textInput = ""
                        showAddTextDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddStickyDialog) {
        AlertDialog(
            onDismissRequest = { showAddStickyDialog = false },
            title = { Text("Add Sticky Note Card") },
            text = {
                OutlinedTextField(
                    value = stickyInput,
                    onValueChange = { stickyInput = it },
                    label = { Text("Sticky Note Idea") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (stickyInput.isNotBlank()) {
                        pushUndoSnapshot()
                        elements.add(
                            CanvaElement.StickyItem(
                                x = 150f,
                                y = 150f,
                                text = stickyInput
                            )
                        )
                        stickyInput = ""
                        showAddStickyDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStickyDialog = false }) { Text("Cancel") }
            }
        )
    }
}
