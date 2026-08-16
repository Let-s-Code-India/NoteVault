package com.example.ui.legal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.platformDisplayName

enum class LegalTab(val title: String) {
    PRIVACY("Privacy Policy"),
    TERMS("Terms & Conditions"),
    DISCLAIMER("Disclaimer"),
    LICENSES("Open Source Licenses"),
    ABOUT("About & Contact")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    initialTab: LegalTab = LegalTab.PRIVACY,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Legal & Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                LegalTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                when (selectedTab) {
                    LegalTab.PRIVACY -> PrivacyPolicySection()
                    LegalTab.TERMS -> TermsConditionsSection()
                    LegalTab.DISCLAIMER -> DisclaimerSection()
                    LegalTab.LICENSES -> OpenSourceLicensesSection()
                    LegalTab.ABOUT -> AboutContactSection()
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicySection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LegalNoticeCard(
            title = "100% Offline & Private by Design",
            icon = Icons.Default.Security,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Published by LET'S CODE INDIA | Platform: $platformDisplayName & Multiplatform",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )

        HorizontalDivider()

        LegalParagraph(
            title = "1. Zero Telemetry & On-Device Data Storage",
            content = "NoteVault, published by LET'S CODE INDIA, is engineered on a strict zero-knowledge, offline-first paradigm. All notes, folders, tags, checklist tasks, logic board diagrams, design canvas compositions, and reminder schedules are stored exclusively on your local device storage. No user accounts, registration, analytics tracking, or telemetry mechanisms exist within NoteVault."
        )

        LegalParagraph(
            title = "2. Database & Data Encryption at Rest",
            content = "Your local database is protected using industry-standard 256-bit AES encryption at rest. Security PINs are preserved using salted SHA-256 cryptographic hashing. Biometric credentials (Fingerprint / Face ID) are handled entirely through your operating system's secure enclave / hardware security module and are never accessible to or transmitted by the application."
        )

        LegalParagraph(
            title = "3. Optional Generative AI Diagram Feature",
            content = "NoteVault includes an optional AI-assisted diagram generation feature. When and ONLY when you explicitly type a prompt and press 'Generate AI Diagram', that specific prompt string is transmitted to the Gemini API service solely to generate node & connection metadata. No existing notes, vault contents, or personal identifiers are attached or transmitted. If you do not use this button, zero network data is ever transmitted."
        )

        LegalParagraph(
            title = "4. System Permissions Policy",
            content = "NoteVault only requests system permissions strictly necessary for user-initiated device features (such as notifications for scheduled reminders, camera for photo capture, and biometric sensors for PIN-less unlock). All permissions are handled natively and remain entirely local."
        )

        LegalParagraph(
            title = "5. Data Retention, Backups & Contact",
            content = "Because NoteVault does not maintain external cloud servers or databases, you retain 100% custody and control over your data. For any questions regarding privacy or data handling, contact LET'S CODE INDIA directly at support@letscodeindia.in."
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "© LET'S CODE INDIA. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun TermsConditionsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Terms & Conditions",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Published by LET'S CODE INDIA | Effective: August 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )

        HorizontalDivider()

        LegalParagraph(
            title = "1. Acceptance of Terms",
            content = "By downloading, installing, or using NoteVault on Android, iOS, Windows, macOS, or Linux, you agree to be bound by these Terms & Conditions provided by LET'S CODE INDIA. If you do not agree, please discontinue use and uninstall the software."
        )

        LegalParagraph(
            title = "2. License & Permitted Use",
            content = "NoteVault grants you a personal, worldwide, non-exclusive, non-transferable license to create, edit, organize, diagram, and export personal and commercial notes and visual assets in accordance with local laws."
        )

        LegalParagraph(
            title = "3. User Responsibility for Backups",
            content = "NoteVault functions as a local, on-device utility. You are solely responsible for creating regular offline backups (.zip archives or Markdown exports) of your data. LET'S CODE INDIA shall not be liable for any data loss resulting from device failure, operating system updates, accidental deletion, or forgotten PINs."
        )

        LegalParagraph(
            title = "4. Inquiries & Support",
            content = "For support or terms inquiries, reach out to our team at support@letscodeindia.in."
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "© LET'S CODE INDIA. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun DisclaimerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Disclaimer & Limitation of Liability",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "As-Is Warranty Provision | LET'S CODE INDIA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )

        HorizontalDivider()

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "IMPORTANT NOTICE:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR PUBLISHERS OF LET'S CODE INDIA BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "© LeCoThIn. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun OpenSourceLicensesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Open Source Licenses & Attributions",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "NoteVault is built using modern open-source multiplatform technologies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        val libraries = listOf(
            Triple("Jetpack / Compose Multiplatform", "JetBrains & Google", "Apache License 2.0"),
            Triple("Kotlin Coroutines & Flow", "JetBrains", "Apache License 2.0"),
            Triple("Room Database & SQLCipher", "Google & Zetetic LLC", "Apache 2.0 & BSD"),
            Triple("Material Design 3 (M3)", "Google LLC", "Apache License 2.0"),
            Triple("Coil Image Loading", "Coil Contributors", "Apache License 2.0"),
            Triple("Moshi Kotlin JSON", "Square Inc.", "Apache License 2.0"),
            Triple("AndroidX Security & Biometrics", "Google LLC", "Apache License 2.0")
        )

        libraries.forEach { (name, author, license) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Author: $author", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            license,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "© LeCoThIn. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun AboutContactSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "About NoteVault",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column {
                        Text("NoteVault Multiplatform", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Version 1.0.0 (Universal Release)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider()

                Text(
                    "Universal local-first knowledge base, Markdown editor, logic board diagram engine, task manager, and encrypted vault running natively on Android, iOS, and Desktop.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Publisher & Organization info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        "Publisher: LET'S CODE INDIA",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text("Developer & Support Contact:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("support@letscodeindia.in", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "© LET'S CODE INDIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Powered by LET'S CODE INDIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalNoticeCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
private fun LegalParagraph(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Text(
            content,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
