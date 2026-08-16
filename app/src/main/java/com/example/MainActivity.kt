package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.platform.initAndroidPlatformContext
import com.example.ui.canvas.LogicBoardScreen
import com.example.ui.components.NoteVaultBottomNavigation
import com.example.ui.components.NoteVaultNavigationRail
import com.example.ui.legal.LegalScreen
import com.example.ui.legal.OnboardingConsentScreen
import com.example.ui.notes.NoteEditScreen
import com.example.ui.notes.NotesScreen
import com.example.ui.reminder.RemindersScreen
import com.example.ui.settings.SettingsVaultScreen
import com.example.ui.tasks.TasksScreen
import com.example.ui.theme.NoteVaultTheme
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.NoteVaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAndroidPlatformContext(this)
        enableEdgeToEdge()
        setContent {
            NoteVaultTheme {
                NoteVaultApp()
            }
        }
    }
}

@Composable
fun NoteVaultApp(viewModel: NoteVaultViewModel = viewModel()) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val hasConsented by viewModel.hasConsentedOnboarding.collectAsState()
    val activeLegalTab by viewModel.activeLegalTab.collectAsState()
    val notificationMessage by viewModel.userNotification.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notificationMessage) {
        notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // One-Time First Launch Onboarding Consent Gate
    if (!hasConsented && currentDestination != AppNavDestination.LEGAL) {
        OnboardingConsentScreen(
            onConsentAgreed = { viewModel.agreeToOnboardingConsent() },
            onViewLegalTab = { tab -> viewModel.openLegalScreen(tab) }
        )
        return
    }

    if (currentDestination == AppNavDestination.LEGAL) {
        LegalScreen(
            initialTab = activeLegalTab,
            onBack = { viewModel.exitLegalScreen() }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val isImmersiveEditor = currentDestination == AppNavDestination.NOTE_EDIT || currentDestination == AppNavDestination.CANVAS_EDIT

        if (isWideScreen && !isImmersiveEditor) {
            // Adaptive Two-Pane / Navigation Rail layout for Tablets, Desktops, iPads & Foldables
            Row(modifier = Modifier.fillMaxSize()) {
                NoteVaultNavigationRail(
                    currentDestination = currentDestination,
                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentDestination, label = "WideScreenTransition") { destination ->
                            when (destination) {
                                AppNavDestination.NOTES -> NotesScreen(viewModel = viewModel)
                                AppNavDestination.NOTE_EDIT -> NoteEditScreen(viewModel = viewModel)
                                AppNavDestination.LOGIC_BOARD, AppNavDestination.CANVAS_EDIT -> LogicBoardScreen(viewModel = viewModel)
                                AppNavDestination.TASKS -> TasksScreen(viewModel = viewModel)
                                AppNavDestination.REMINDERS -> RemindersScreen(viewModel = viewModel)
                                AppNavDestination.SETTINGS_VAULT -> SettingsVaultScreen(viewModel = viewModel)
                                AppNavDestination.LEGAL -> LegalScreen(initialTab = activeLegalTab, onBack = { viewModel.exitLegalScreen() })
                            }
                        }
                    }
                }
            }
        } else {
            // Handheld Mobile / Phone layout with floating bottom navigation
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isImmersiveEditor) {
                        NoteVaultBottomNavigation(
                            currentDestination = currentDestination,
                            onNavigate = { dest -> viewModel.navigateTo(dest) }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isImmersiveEditor) PaddingValues() else innerPadding)
                ) {
                    Crossfade(targetState = currentDestination, label = "ScreenTransition") { destination ->
                        when (destination) {
                            AppNavDestination.NOTES -> NotesScreen(viewModel = viewModel)
                            AppNavDestination.NOTE_EDIT -> NoteEditScreen(viewModel = viewModel)
                            AppNavDestination.LOGIC_BOARD, AppNavDestination.CANVAS_EDIT -> LogicBoardScreen(viewModel = viewModel)
                            AppNavDestination.TASKS -> TasksScreen(viewModel = viewModel)
                            AppNavDestination.REMINDERS -> RemindersScreen(viewModel = viewModel)
                            AppNavDestination.SETTINGS_VAULT -> SettingsVaultScreen(viewModel = viewModel)
                            AppNavDestination.LEGAL -> LegalScreen(initialTab = activeLegalTab, onBack = { viewModel.exitLegalScreen() })
                        }
                    }
                }
            }
        }
    }
}
