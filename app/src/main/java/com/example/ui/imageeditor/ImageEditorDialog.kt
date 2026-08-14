package com.example.ui.imageeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorDialog(
    initialBitmap: Bitmap,
    onDismiss: () -> Unit,
    onSaveEdited: (Bitmap) -> Unit
) {
    var currentBitmap by remember { mutableStateOf(initialBitmap) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var isDrawMode by remember { mutableStateOf(false) }
    var drawColor by remember { mutableStateOf(Color.Red) }
    val paths = remember { mutableStateListOf<Pair<Path, Int>>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Editor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            // Apply final draw annotations to bitmap if needed
                            val finalBmp = currentBitmap.copy(currentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                            if (paths.isNotEmpty()) {
                                val canvas = Canvas(finalBmp)
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    style = Paint.Style.STROKE
                                    strokeWidth = 12f
                                }
                                for ((path, colInt) in paths) {
                                    paint.color = colInt
                                    canvas.drawPath(path, paint)
                                }
                            }
                            onSaveEdited(finalBmp)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Done")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Brightness slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Brightness", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                        Slider(
                            value = brightness,
                            onValueChange = {
                                brightness = it
                                currentBitmap = ImageProcessingUtils.adjustBrightnessContrast(initialBitmap, brightness, contrast)
                            },
                            valueRange = -100f..100f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = {
                            currentBitmap = ImageProcessingUtils.rotateBitmap(currentBitmap, 90f)
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                                Text("Rotate", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        IconButton(onClick = { isDrawMode = !isDrawMode }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Annotate",
                                    tint = if (isDrawMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Draw", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (isDrawMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow).forEach { color ->
                                    IconButton(
                                        onClick = { drawColor = color },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color, RoundedCornerShape(16.dp))
                                    ) {}
                                }
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
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                if (isDrawMode) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val p = Path().apply { moveTo(offset.x, offset.y) }
                                        currentPath = p
                                        paths.add(p to drawColor.toArgb())
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentPath?.lineTo(change.position.x, change.position.y)
                                    }
                                )
                            }
                    ) {
                        // Drawing overlay preview
                    }
                }
            }
        }
    }
}
