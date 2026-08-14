package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DiagramEdge
import com.example.data.model.DiagramEntity
import com.example.data.model.DiagramNode
import com.example.data.model.NoteEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ExportManager {

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // --- Notes PDF Export ---
    fun exportNoteAsPdf(context: Context, note: NoteEntity): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        val paintMeta = Paint().apply {
            color = Color.GRAY
            textSize = 11f
        }
        val paintHeading = Paint().apply {
            color = Color.rgb(30, 58, 138)
            textSize = 16f
            isFakeBoldText = true
        }
        val paintBody = Paint().apply {
            color = Color.rgb(31, 41, 55)
            textSize = 12f
        }

        var y = 50f
        val margin = 40f
        val maxLineWidth = 515f

        // Title
        canvas.drawText(note.title.ifBlank { "Untitled Note" }, margin, y, paintTitle)
        y += 24f

        // Metadata
        val metaStr = "Tags: ${note.tags.ifBlank { "None" }} | Updated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.updatedAt))}"
        canvas.drawText(metaStr, margin, y, paintMeta)
        y += 16f

        canvas.drawLine(margin, y, margin + maxLineWidth, y, paintMeta)
        y += 24f

        // Content parsing
        val lines = note.content.lines()
        var pageNum = 1

        for (line in lines) {
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNum++
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(nextInfo)
                canvas = page.canvas
                y = 50f
            }

            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> {
                    canvas.drawText(trimmed.removePrefix("# "), margin, y, paintHeading)
                    y += 22f
                }
                trimmed.startsWith("## ") -> {
                    canvas.drawText(trimmed.removePrefix("## "), margin, y, paintHeading)
                    y += 20f
                }
                trimmed.startsWith("- [ ] ") -> {
                    canvas.drawText("☐ " + trimmed.removePrefix("- [ ] "), margin + 10f, y, paintBody)
                    y += 18f
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") -> {
                    canvas.drawText("☑ " + trimmed.removePrefix("- [x] ").removePrefix("- [X] "), margin + 10f, y, paintBody)
                    y += 18f
                }
                trimmed.startsWith("- ") -> {
                    canvas.drawText("• " + trimmed.removePrefix("- "), margin + 10f, y, paintBody)
                    y += 18f
                }
                else -> {
                    // Wrap long text
                    val words = line.split(" ")
                    var currentLine = ""
                    for (word in words) {
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (paintBody.measureText(testLine) > maxLineWidth) {
                            canvas.drawText(currentLine, margin, y, paintBody)
                            y += 18f
                            currentLine = word
                        } else {
                            currentLine = testLine
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        canvas.drawText(currentLine, margin, y, paintBody)
                        y += 18f
                    }
                }
            }
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "${note.title.take(20).replace(" ", "_")}_export.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }

    // --- Notes HTML Export ---
    fun exportNoteAsHtml(context: Context, note: NoteEntity): File {
        val htmlContent = buildString {
            append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            append("<title>${note.title}</title>")
            append("<style>")
            append("body { font-family: -apple-system, sans-serif; margin: 40px; background: #fafafa; color: #111827; }")
            append("h1 { color: #1e3a8a; border-bottom: 2px solid #e5e7eb; padding-bottom: 8px; }")
            append(".meta { color: #6b7280; font-size: 14px; margin-bottom: 24px; }")
            append(".content { background: white; padding: 24px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }")
            append("li { margin-bottom: 6px; }")
            append("</style></head><body>")
            append("<h1>${note.title}</h1>")
            append("<div class='meta'>Tags: ${note.tags} | Updated: ${java.util.Date(note.updatedAt)}</div>")
            append("<div class='content'>")

            val lines = note.content.lines()
            for (line in lines) {
                val t = line.trim()
                when {
                    t.startsWith("# ") -> append("<h2>${t.removePrefix("# ")}</h2>")
                    t.startsWith("## ") -> append("<h3>${t.removePrefix("## ")}</h3>")
                    t.startsWith("- [ ] ") -> append("<div><input type='checkbox' disabled> ${t.removePrefix("- [ ] ")}</div>")
                    t.startsWith("- [x] ") -> append("<div><input type='checkbox' checked disabled> ${t.removePrefix("- [x] ")}</div>")
                    t.startsWith("- ") -> append("<li>${t.removePrefix("- ")}</li>")
                    else -> if (t.isNotBlank()) append("<p>$t</p>")
                }
            }
            append("</div></body></html>")
        }

        val file = File(context.cacheDir, "${note.title.take(20).replace(" ", "_")}_export.html")
        file.writeText(htmlContent)
        return file
    }

    // --- Notes Presentation Slides Export (PPT / HTML Presentation Deck) ---
    fun exportNoteAsPresentationSlides(context: Context, note: NoteEntity): File {
        val slides = mutableListOf<Pair<String, List<String>>>()
        val lines = note.content.lines()
        var currentSlideTitle = note.title.ifBlank { "Presentation" }
        var currentSlidePoints = mutableListOf<String>()

        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("# ") || t.startsWith("## ")) {
                if (currentSlidePoints.isNotEmpty() || currentSlideTitle != note.title) {
                    slides.add(currentSlideTitle to currentSlidePoints.toList())
                    currentSlidePoints = mutableListOf()
                }
                currentSlideTitle = t.removePrefix("# ").removePrefix("## ").trim()
            } else if (t.isNotBlank()) {
                currentSlidePoints.add(t)
            }
        }
        if (currentSlidePoints.isNotEmpty() || slides.isEmpty()) {
            slides.add(currentSlideTitle to currentSlidePoints.toList())
        }

        val htmlContent = buildString {
            append("<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            append("<title>${note.title} - Presentation Slides</title>")
            append("<style>")
            append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 0; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }")
            append(".slide-container { flex: 1; display: flex; align-items: center; justify-content: center; padding: 24px; }")
            append(".slide { background: linear-gradient(135deg, #1E293B, #0F172A); border: 1px solid #334155; border-radius: 24px; padding: 48px; width: 100%; max-width: 960px; min-height: 520px; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); display: none; flex-direction: column; justify-content: space-between; box-sizing: border-box; }")
            append(".slide.active { display: flex; animation: fadeIn 0.3s ease-out; }")
            append("@keyframes fadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }")
            append(".slide-header { border-bottom: 2px solid #3B82F6; padding-bottom: 16px; margin-bottom: 24px; }")
            append(".slide-title { font-size: 32px; font-weight: 800; color: #60A5FA; margin: 0; }")
            append(".slide-body { flex: 1; font-size: 20px; line-height: 1.6; color: #E2E8F0; }")
            append(".slide-point { margin-bottom: 12px; display: flex; align-items: flex-start; }")
            append(".bullet { color: #38BDF8; font-weight: bold; margin-right: 12px; font-size: 24px; line-height: 1; }")
            append(".slide-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #334155; padding-top: 16px; font-size: 14px; color: #94A3B8; }")
            append(".controls { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 16px 24px; background: #1E293B; border-top: 1px solid #334155; }")
            append(".btn { background: #3B82F6; color: white; border: none; padding: 10px 20px; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; transition: all 0.2s; }")
            append(".btn:hover { background: #2563EB; }")
            append(".btn:disabled { background: #475569; cursor: not-allowed; }")
            append(".counter { font-size: 16px; font-weight: 600; color: #94A3B8; }")
            append("</style></head><body>")

            append("<div class='slide-container'>")
            slides.forEachIndexed { index, (title, points) ->
                val activeClass = if (index == 0) "active" else ""
                append("<div class='slide $activeClass' id='slide-$index'>")
                append("<div class='slide-header'><h1 class='slide-title'>$title</h1></div>")
                append("<div class='slide-body'>")
                points.forEach { point ->
                    val clean = point.removePrefix("- ").removePrefix("* ").removePrefix("- [ ] ").removePrefix("- [x] ")
                    append("<div class='slide-point'><span class='bullet'>✦</span><span>$clean</span></div>")
                }
                append("</div>")
                append("<div class='slide-footer'><span>${note.title}</span><span>Slide ${index + 1} of ${slides.size}</span></div>")
                append("</div>")
            }
            append("</div>")

            append("<div class='controls'>")
            append("<button class='btn' id='prevBtn' onclick='prevSlide()'>← Previous</button>")
            append("<span class='counter' id='slideCounter'>1 / ${slides.size}</span>")
            append("<button class='btn' id='nextBtn' onclick='nextSlide()'>Next →</button>")
            append("</div>")

            append("<script>")
            append("let current = 0; const total = ${slides.size};")
            append("function updateSlide(n) {")
            append("  document.querySelectorAll('.slide').forEach(s => s.classList.remove('active'));")
            append("  document.getElementById('slide-' + n).classList.add('active');")
            append("  document.getElementById('slideCounter').innerText = (n + 1) + ' / ' + total;")
            append("  document.getElementById('prevBtn').disabled = n === 0;")
            append("  document.getElementById('nextBtn').disabled = n === total - 1;")
            append("}")
            append("function nextSlide() { if (current < total - 1) { current++; updateSlide(current); } }")
            append("function prevSlide() { if (current > 0) { current--; updateSlide(current); } }")
            append("document.addEventListener('keydown', e => { if (e.key === 'ArrowRight' || e.key === ' ') nextSlide(); if (e.key === 'ArrowLeft') prevSlide(); });")
            append("updateSlide(0);")
            append("</script></body></html>")
        }

        val file = File(context.cacheDir, "${note.title.take(20).replace(" ", "_")}_slides.html")
        file.writeText(htmlContent)
        return file
    }

    // --- Notes Markdown (.md) Export ---
    fun exportNoteAsMarkdownFile(context: Context, note: NoteEntity): File {
        val mdContent = buildString {
            append("---\n")
            append("title: \"${note.title}\"\n")
            append("tags: [${note.tags.split(",").joinToString { "\"${it.trim()}\"" }}]\n")
            append("updated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(note.updatedAt))}\n")
            append("---\n\n")
            append("# ${note.title}\n\n")
            append(note.content)
        }
        val file = File(context.cacheDir, "${note.title.take(20).replace(" ", "_")}.md")
        file.writeText(mdContent)
        return file
    }

    // --- Notes Plain Text (.txt) Export ---
    fun exportNoteAsPlainText(context: Context, note: NoteEntity): File {
        val textContent = buildString {
            append("${note.title.uppercase()}\n")
            append("=".repeat(note.title.length.coerceAtLeast(10)) + "\n")
            append("Tags: ${note.tags}\n")
            append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.updatedAt))}\n\n")
            append(note.content)
        }
        val file = File(context.cacheDir, "${note.title.take(20).replace(" ", "_")}.txt")
        file.writeText(textContent)
        return file
    }
    fun exportFolderAsPdf(context: Context, folderName: String, notes: List<NoteEntity>): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1

        for (note in notes) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paintTitle = Paint().apply { color = Color.BLACK; textSize = 20f; isFakeBoldText = true }
            val paintBody = Paint().apply { color = Color.rgb(55, 65, 81); textSize = 12f }

            var y = 50f
            canvas.drawText("Folder: $folderName - ${note.title}", 40f, y, paintTitle)
            y += 30f

            for (line in note.content.lines().take(35)) {
                canvas.drawText(line.take(70), 40f, y, paintBody)
                y += 18f
            }

            pdfDocument.finishPage(page)
            pageNum++
        }

        val file = File(context.cacheDir, "Folder_${folderName.replace(" ", "_")}_Batch.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }

    fun exportFolderAsZip(context: Context, folderName: String, notes: List<NoteEntity>): File {
        val zipFile = File(context.cacheDir, "Folder_${folderName.replace(" ", "_")}_Markdown.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (note in notes) {
                val entryName = "${note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.md"
                zos.putNextEntry(ZipEntry(entryName))
                val content = "# ${note.title}\n\nTags: ${note.tags}\n\n${note.content}"
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return zipFile
    }

    // --- Diagram Raster (PNG) & Vector (SVG) Exports ---
    fun exportDiagramAsPng(
        context: Context,
        diagram: DiagramEntity,
        nodes: List<DiagramNode>,
        edges: List<DiagramEdge>
    ): File {
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.rgb(248, 250, 252))

        val paintNode = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val paintNodeBorder = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(100, 116, 139) }
        val paintTextTitle = Paint().apply { isAntiAlias = true; color = Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val paintTextSub = Paint().apply { isAntiAlias = true; color = Color.rgb(100, 116, 139); textSize = 12f }
        val paintEdge = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.rgb(148, 163, 184) }

        val nodeMap = nodes.associateBy { it.id }

        // Draw edges first
        for (edge in edges) {
            val from = nodeMap[edge.fromNodeId] ?: continue
            val to = nodeMap[edge.toNodeId] ?: continue
            val startX = from.x + from.width / 2f
            val startY = from.y + from.height / 2f
            val endX = to.x + to.width / 2f
            val endY = to.y + to.height / 2f

            try {
                paintEdge.color = Color.parseColor(edge.colorHex)
            } catch (e: Exception) {
                paintEdge.color = Color.rgb(148, 163, 184)
            }
            canvas.drawLine(startX, startY, endX, endY, paintEdge)
        }

        // Draw nodes
        for (node in nodes) {
            val rect = RectF(node.x, node.y, node.x + node.width, node.y + node.height)
            try {
                paintNode.color = Color.parseColor(node.colorHex)
            } catch (e: Exception) {
                paintNode.color = Color.rgb(59, 130, 246)
            }

            canvas.drawRoundRect(rect, 16f, 16f, paintNode)
            canvas.drawRoundRect(rect, 16f, 16f, paintNodeBorder)

            // Text
            canvas.drawText(node.label.take(20), node.x + 12f, node.y + 32f, paintTextTitle)
            if (node.subText.isNotBlank()) {
                canvas.drawText(node.subText.take(24), node.x + 12f, node.y + 54f, paintTextSub)
            }
        }

        val file = File(context.cacheDir, "${diagram.title.replace(" ", "_")}_canvas.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun exportDiagramAsSvg(
        context: Context,
        diagram: DiagramEntity,
        nodes: List<DiagramNode>,
        edges: List<DiagramEdge>
    ): File {
        val svg = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<svg width=\"1920\" height=\"1080\" xmlns=\"http://www.w3.org/2000/svg\">\n")
            append("<rect width=\"100%\" height=\"100%\" fill=\"#F8FAFC\"/>\n")

            val nodeMap = nodes.associateBy { it.id }

            // Edges
            for (e in edges) {
                val from = nodeMap[e.fromNodeId] ?: continue
                val to = nodeMap[e.toNodeId] ?: continue
                val x1 = from.x + from.width / 2f
                val y1 = from.y + from.height / 2f
                val x2 = to.x + to.width / 2f
                val y2 = to.y + to.height / 2f
                append("<line x1=\"$x1\" y1=\"$y1\" x2=\"$x2\" y2=\"$y2\" stroke=\"${e.colorHex}\" stroke-width=\"3\"/>\n")
            }

            // Nodes
            for (n in nodes) {
                append("<rect x=\"${n.x}\" y=\"${n.y}\" width=\"${n.width}\" height=\"${n.height}\" rx=\"12\" fill=\"${n.colorHex}\" stroke=\"#64748B\" stroke-width=\"2\"/>\n")
                append("<text x=\"${n.x + 12}\" y=\"${n.y + 30}\" font-family=\"sans-serif\" font-size=\"14\" font-weight=\"bold\" fill=\"#FFFFFF\">${n.label}</text>\n")
                if (n.subText.isNotBlank()) {
                    append("<text x=\"${n.x + 12}\" y=\"${n.y + 50}\" font-family=\"sans-serif\" font-size=\"11\" fill=\"#F1F5F9\">${n.subText}</text>\n")
                }
            }
            append("</svg>")
        }

        val file = File(context.cacheDir, "${diagram.title.replace(" ", "_")}_vector.svg")
        file.writeText(svg)
        return file
    }

    // --- Diagram Mermaid Markdown Export (.mmd) ---
    fun exportDiagramAsMermaid(
        context: Context,
        diagram: DiagramEntity,
        nodes: List<DiagramNode>,
        edges: List<DiagramEdge>
    ): File {
        val mermaid = buildString {
            append("%% Mermaid Flowchart Export\n")
            append("graph TD\n")
            val nodeMap = nodes.associateBy { it.id }
            for (node in nodes) {
                val safeLabel = node.label.replace("\"", "'")
                val sanitizedId = "node_${node.id.replace("-", "").take(8)}"
                when (node.type) {
                    "DIAMOND", "CONDITION" -> append("    $sanitizedId{\"$safeLabel\"}\n")
                    "CIRCLE" -> append("    $sanitizedId((\"$safeLabel\"))\n")
                    "DATABASE" -> append("    $sanitizedId[(\"$safeLabel\")]\n")
                    "ROUNDED_CARD", "STICKY" -> append("    $sanitizedId(\"$safeLabel\")\n")
                    else -> append("    $sanitizedId[\"$safeLabel\"]\n")
                }
            }
            for (edge in edges) {
                val from = nodeMap[edge.fromNodeId] ?: continue
                val to = nodeMap[edge.toNodeId] ?: continue
                val fromId = "node_${from.id.replace("-", "").take(8)}"
                val toId = "node_${to.id.replace("-", "").take(8)}"
                if (edge.label.isNotBlank()) {
                    append("    $fromId -->|\"${edge.label}\"| $toId\n")
                } else {
                    append("    $fromId --> $toId\n")
                }
            }
        }
        val file = File(context.cacheDir, "${diagram.title.replace(" ", "_")}.mmd")
        file.writeText(mermaid)
        return file
    }

    // --- Full App Zip Backup with Images ---
    fun exportFullBackupZip(context: Context, backupJson: String): File {
        val zipFile = File(context.cacheDir, "NoteVault_Full_Backup_${System.currentTimeMillis()}.zip")
        val imageDir = File(context.filesDir, "images")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // backup.json
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(backupJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // images folder
            if (imageDir.exists() && imageDir.isDirectory) {
                imageDir.listFiles()?.forEach { img ->
                    if (img.isFile) {
                        zos.putNextEntry(ZipEntry("images/${img.name}"))
                        img.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        return zipFile
    }

    fun restoreFullBackupZip(context: Context, zipInputStream: InputStream): String? {
        var jsonContent: String? = null
        val destImageDir = File(context.filesDir, "images")
        if (!destImageDir.exists()) destImageDir.mkdirs()

        ZipInputStream(zipInputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "backup.json") {
                    jsonContent = zis.bufferedReader().readText()
                } else if (entry.name.startsWith("images/") && !entry.isDirectory) {
                    val imageName = entry.name.removePrefix("images/")
                    val imgFile = File(destImageDir, imageName)
                    FileOutputStream(imgFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return jsonContent
    }
}
