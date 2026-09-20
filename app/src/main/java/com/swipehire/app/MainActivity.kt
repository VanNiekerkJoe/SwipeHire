package com.swipehire.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipehire.app.ui.theme.SwipeHireTheme
import com.swipehire.app.ui.LocalAppLanguage
import com.swipehire.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.state.collectAsState()

            CompositionLocalProvider(LocalAppLanguage provides settingsState.language) {
                SwipeHireTheme(themeMode = settingsState.themeMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        SwipeHireApp(settingsViewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}
