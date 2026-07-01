package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SummitApp
import com.example.ui.SummitViewModel
import com.example.ui.theme.SummitTheme
import com.example.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SummitViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()

            androidx.compose.runtime.LaunchedEffect(themeMode, systemDark) {
                ThemeState.isDark = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> systemDark
                }
            }

            SummitTheme {
                SummitApp(viewModel = viewModel)
            }
        }
    }
}
