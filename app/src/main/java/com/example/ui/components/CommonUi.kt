package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGradient
import com.example.ui.viewmodel.AppNavDestination

@Composable
fun NoteVaultBottomNavigation(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(72.dp)
        ) {
            val items = listOf(
                NavigationItemData(
                    destination = AppNavDestination.NOTES,
                    label = "Notes",
                    activeIcon = Icons.Filled.Description,
                    inactiveIcon = Icons.Outlined.Description,
                    isSelected = currentDestination == AppNavDestination.NOTES || currentDestination == AppNavDestination.NOTE_EDIT
                ),
                NavigationItemData(
                    destination = AppNavDestination.LOGIC_BOARD,
                    label = "Logic Board",
                    activeIcon = Icons.Filled.AccountTree,
                    inactiveIcon = Icons.Outlined.AccountTree,
                    isSelected = currentDestination == AppNavDestination.LOGIC_BOARD || currentDestination == AppNavDestination.CANVAS_EDIT
                ),
                NavigationItemData(
                    destination = AppNavDestination.TASKS,
                    label = "Tasks",
                    activeIcon = Icons.Filled.CheckCircle,
                    inactiveIcon = Icons.Outlined.CheckCircleOutline,
                    isSelected = currentDestination == AppNavDestination.TASKS
                ),
                NavigationItemData(
                    destination = AppNavDestination.REMINDERS,
                    label = "Reminders",
                    activeIcon = Icons.Filled.Notifications,
                    inactiveIcon = Icons.Outlined.Notifications,
                    isSelected = currentDestination == AppNavDestination.REMINDERS
                ),
                NavigationItemData(
                    destination = AppNavDestination.SETTINGS_VAULT,
                    label = "Vault",
                    activeIcon = Icons.Filled.Lock,
                    inactiveIcon = Icons.Outlined.Lock,
                    isSelected = currentDestination == AppNavDestination.SETTINGS_VAULT
                )
            )

            items.forEach { item ->
                NavigationBarItem(
                    selected = item.isSelected,
                    onClick = { onNavigate(item.destination) },
                    icon = {
                        Icon(
                            if (item.isSelected) item.activeIcon else item.inactiveIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@Composable
fun NoteVaultNavigationRail(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxHeight(),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "NoteVault Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    "NoteVault",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    ) {
        val items = listOf(
            NavigationItemData(
                destination = AppNavDestination.NOTES,
                label = "Notes",
                activeIcon = Icons.Filled.Description,
                inactiveIcon = Icons.Outlined.Description,
                isSelected = currentDestination == AppNavDestination.NOTES || currentDestination == AppNavDestination.NOTE_EDIT
            ),
            NavigationItemData(
                destination = AppNavDestination.LOGIC_BOARD,
                label = "Logic Board",
                activeIcon = Icons.Filled.AccountTree,
                inactiveIcon = Icons.Outlined.AccountTree,
                isSelected = currentDestination == AppNavDestination.LOGIC_BOARD || currentDestination == AppNavDestination.CANVAS_EDIT
            ),
            NavigationItemData(
                destination = AppNavDestination.TASKS,
                label = "Tasks",
                activeIcon = Icons.Filled.CheckCircle,
                inactiveIcon = Icons.Outlined.CheckCircleOutline,
                isSelected = currentDestination == AppNavDestination.TASKS
            ),
            NavigationItemData(
                destination = AppNavDestination.REMINDERS,
                label = "Reminders",
                activeIcon = Icons.Filled.Notifications,
                inactiveIcon = Icons.Outlined.Notifications,
                isSelected = currentDestination == AppNavDestination.REMINDERS
            ),
            NavigationItemData(
                destination = AppNavDestination.SETTINGS_VAULT,
                label = "Vault",
                activeIcon = Icons.Filled.Lock,
                inactiveIcon = Icons.Outlined.Lock,
                isSelected = currentDestination == AppNavDestination.SETTINGS_VAULT
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        items.forEach { item ->
            NavigationRailItem(
                selected = item.isSelected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        if (item.isSelected) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private data class NavigationItemData(
    val destination: AppNavDestination,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteVaultTopBar(
    title: String,
    syncStatus: String,
    onMenuClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseSync")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        },
        navigationIcon = {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Folder Drawer", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
fun PinEntryDialog(
    title: String = "Vault Security Lock",
    onPinSubmitted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Enter your 4-6 digit security PIN to access protected notes and encrypted vault files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Stylized PIN indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pinText.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Numeric Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    val keypadRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    keypadRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { key ->
                                Surface(
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                pinText = ""
                                                errorMsg = null
                                            }
                                            "⌫" -> {
                                                if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                                errorMsg = null
                                            }
                                            else -> {
                                                if (pinText.length < 6) {
                                                    pinText += key
                                                    errorMsg = null
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length >= 4) {
                        onPinSubmitted(pinText)
                    } else {
                        errorMsg = "PIN must be at least 4 digits"
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onFolderCreated: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val colors = listOf("#6366F1", "#EC4899", "#10B981", "#F59E0B", "#06B6D4", "#8B5CF6", "#3B82F6", "#EF4444")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("New Folder", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. Work, Ideas, Architecture") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color Accent", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onFolderCreated(folderName.trim(), selectedColor)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = folderName.isNotBlank()
            ) {
                Text("Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
