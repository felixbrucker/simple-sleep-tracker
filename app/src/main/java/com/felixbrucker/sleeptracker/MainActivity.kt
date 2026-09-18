package com.felixbrucker.sleeptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felixbrucker.sleeptracker.ui.screens.SleepTrackerScreen
import com.felixbrucker.sleeptracker.ui.viewmodel.SleepViewModel
import com.felixbrucker.sleeptracker.ui.theme.SleepTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SleepTrackerTheme {
                val viewModel: SleepViewModel = viewModel(
                    factory = SleepViewModel.Factory(application)
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SleepTrackerScreen(
                        uiState = uiState,
                        onStartTracking = viewModel::startTracking,
                        onStopTracking = viewModel::stopTracking,
                        onUpdateReminder = viewModel::updateReminderConfig,
                        onTestReminder = viewModel::sendTestReminder,
                        onSetAutoSync = viewModel::setAutoSyncHealthConnect,
                        onSyncSession = viewModel::syncSession,
                        onSyncAll = viewModel::syncAllUnsynced,
                        onDeleteSession = viewModel::deleteSession,
                        onUpdateSession = viewModel::updateSession,
                        onSetFallAsleepDuration = viewModel::setFallAsleepDurationMinutes,
                        onRefreshHealthConnect = viewModel::refreshHealthConnectPermissions,
                        onClearStatusMessage = viewModel::clearStatusMessage
                    )
                }
            }
        }
    }
}
