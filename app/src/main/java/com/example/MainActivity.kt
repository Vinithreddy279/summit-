package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SummitApp
import com.example.ui.SummitViewModel
import com.example.ui.theme.SummitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SummitTheme {
                val viewModel: SummitViewModel = viewModel()
                SummitApp(viewModel = viewModel)
            }
        }
    }
}
