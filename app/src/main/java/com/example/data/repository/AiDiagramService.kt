package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.DiagramEdge
import com.example.data.model.DiagramNode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GeneratedDiagramResult(
    val title: String,
    val description: String,
    val nodes: List<DiagramNode>,
    val edges: List<DiagramEdge>
)

class AiDiagramService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateDiagramFromPrompt(prompt: String): GeneratedDiagramResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.AI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_AI_API_KEY") {
            // Offline or unconfigured fallback generator
            return@withContext createFallbackDiagram(prompt)
        }

        val systemInstruction = """
            You are a system architecture and flowchart visual diagram generator.
            Given a user prompt describing a process, system, or decision tree, respond ONLY with a strict valid JSON object (no markdown formatting, no backticks, no extra text) with the following exact keys:
            {
              "title": "Diagram Title",
              "description": "Brief summary",
              "nodes": [
                {
                  "id": "node_1",
                  "type": "RECTANGLE" or "DIAMOND" or "ROUNDED_CARD" or "CIRCLE" or "CONDITION" or "ACTION" or "OUTCOME",
                  "label": "Short label",
                  "subText": "Details",
                  "x": 100,
                  "y": 100,
                  "width": 160,
                  "height": 90,
                  "colorHex": "#3B82F6"
                }
              ],
              "edges": [
                {
                  "id": "edge_1",
                  "fromNodeId": "node_1",
                  "toNodeId": "node_2",
                  "label": "Edge label (e.g., If yes)",
                  "lineStyle": "ORTHOGONAL",
                  "colorHex": "#94A3B8"
                }
              ]
            }
            Position nodes neatly horizontally or vertically on a grid with coordinates starting around x=100, y=100 with 180-220px step spacing.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "System Instruction:\n$systemInstruction\n\nUser Request: $prompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("responseMimeType", "application/json")
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful || responseBody.isBlank()) {
                return@withContext createFallbackDiagram(prompt)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseDiagramJson(text, prompt)
        } catch (e: Exception) {
            createFallbackDiagram(prompt)
        }
    }

    private fun parseDiagramJson(jsonText: String, prompt: String): GeneratedDiagramResult {
        return try {
            val cleanJson = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)

            val title = obj.optString("title", "Generated Diagram")
            val description = obj.optString("description", prompt)

            val nodesList = mutableListOf<DiagramNode>()
            val jsonNodes = obj.optJSONArray("nodes") ?: JSONArray()
            val nodeMap = mutableMapOf<String, String>() // mapping generated string id to UUID

            for (i in 0 until jsonNodes.length()) {
                val n = jsonNodes.getJSONObject(i)
                val originalId = n.optString("id", "n_$i")
                val realId = UUID.randomUUID().toString()
                nodeMap[originalId] = realId

                nodesList.add(
                    DiagramNode(
                        id = realId,
                        type = n.optString("type", "RECTANGLE"),
                        label = n.optString("label", "Node ${i + 1}"),
                        subText = n.optString("subText", ""),
                        x = n.optDouble("x", 100.0 + (i * 200)).toFloat(),
                        y = n.optDouble("y", 150.0).toFloat(),
                        width = n.optDouble("width", 160.0).toFloat(),
                        height = n.optDouble("height", 90.0).toFloat(),
                        colorHex = n.optString("colorHex", "#3B82F6")
                    )
                )
            }

            val edgesList = mutableListOf<DiagramEdge>()
            val jsonEdges = obj.optJSONArray("edges") ?: JSONArray()
            for (i in 0 until jsonEdges.length()) {
                val e = jsonEdges.getJSONObject(i)
                val fromOrig = e.optString("fromNodeId")
                val toOrig = e.optString("toNodeId")
                val fromReal = nodeMap[fromOrig] ?: continue
                val toReal = nodeMap[toOrig] ?: continue

                edgesList.add(
                    DiagramEdge(
                        id = UUID.randomUUID().toString(),
                        fromNodeId = fromReal,
                        toNodeId = toReal,
                        label = e.optString("label", ""),
                        lineStyle = e.optString("lineStyle", "ORTHOGONAL"),
                        colorHex = e.optString("colorHex", "#94A3B8")
                    )
                )
            }

            GeneratedDiagramResult(title, description, nodesList, edgesList)
        } catch (e: Exception) {
            createFallbackDiagram(prompt)
        }
    }

    fun createFallbackDiagram(prompt: String): GeneratedDiagramResult {
        val n1 = DiagramNode(id = UUID.randomUUID().toString(), type = "CIRCLE", label = "User Input", subText = prompt, x = 100f, y = 200f, colorHex = "#10B981")
        val n2 = DiagramNode(id = UUID.randomUUID().toString(), type = "DIAMOND", label = "Condition Check", subText = "Contains Media?", x = 340f, y = 185f, colorHex = "#F59E0B")
        val n3 = DiagramNode(id = UUID.randomUUID().toString(), type = "ROUNDED_CARD", label = "Text Processing", subText = "Fast LLM Pipeline", x = 600f, y = 100f, colorHex = "#3B82F6")
        val n4 = DiagramNode(id = UUID.randomUUID().toString(), type = "ROUNDED_CARD", label = "Vision Pipeline", subText = "Multimodal Model", x = 600f, y = 280f, colorHex = "#8B5CF6")
        val n5 = DiagramNode(id = UUID.randomUUID().toString(), type = "OUTCOME", label = "Response Rendered", subText = "Sync to NoteVault", x = 860f, y = 190f, colorHex = "#84CC16")

        val e1 = DiagramEdge(fromNodeId = n1.id, toNodeId = n2.id, label = "Incoming request", lineStyle = "ORTHOGONAL")
        val e2 = DiagramEdge(fromNodeId = n2.id, toNodeId = n3.id, label = "If Text Only", lineStyle = "ORTHOGONAL", colorHex = "#3B82F6")
        val e3 = DiagramEdge(fromNodeId = n2.id, toNodeId = n4.id, label = "If Multimodal", lineStyle = "ORTHOGONAL", colorHex = "#8B5CF6")
        val e4 = DiagramEdge(fromNodeId = n3.id, toNodeId = n5.id, label = "Output", lineStyle = "ORTHOGONAL")
        val e5 = DiagramEdge(fromNodeId = n4.id, toNodeId = n5.id, label = "Output", lineStyle = "ORTHOGONAL")

        return GeneratedDiagramResult(
            title = if (prompt.length > 25) prompt.take(25) + "..." else prompt,
            description = "Logic Flow for: $prompt",
            nodes = listOf(n1, n2, n3, n4, n5),
            edges = listOf(e1, e2, e3, e4, e5)
        )
    }
}
