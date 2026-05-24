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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eltavine.duckparse.ui.ParserScreen
import com.eltavine.duckparse.ui.ParserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ParserViewModel = viewModel()
            val sharedImageUri = remember {
                intent?.let {
                    it.clipData?.getItemAt(0)?.uri ?: it.data
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold { padding ->
                        ParserScreen(
                            viewModel = viewModel,
                            sharedImageUri = sharedImageUri,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
