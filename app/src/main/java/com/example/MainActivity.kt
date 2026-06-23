package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FiveMMainLayout
import com.example.ui.FiveMViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable full Edge to Edge screen renderings complying with modern Material design
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FiveMViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.FivemDarkBg
                ) {
                    FiveMMainLayout(viewModel = viewModel)
                }
            }
        }
    }
}
