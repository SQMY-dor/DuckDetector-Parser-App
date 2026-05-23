package com.eltavine.duckparse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.eltavine.duckparse.ui.ParserScreen
import com.eltavine.duckparse.ui.ParserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ParserViewModel()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold { padding ->
                        ParserScreen(
                            viewModel = viewModel,
                            sharedImageUri = intent?.clipData?.getItemAt(0)?.uri
                                ?: intent?.data,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
