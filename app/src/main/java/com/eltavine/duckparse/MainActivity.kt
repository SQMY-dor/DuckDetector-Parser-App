package com.eltavine.duckparse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import com.eltavine.duckparse.ui.ParserScreen
import com.eltavine.duckparse.ui.ParserViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ParserViewModel()

        setContent {
            MaterialTheme(
                colorScheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
                    dynamicLightColorScheme(this)
                } else {
                    lightColorScheme()
                },
            ) {
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
