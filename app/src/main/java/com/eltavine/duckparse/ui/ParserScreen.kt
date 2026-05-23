package com.eltavine.duckparse.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eltavine.duckparse.model.DeviceInfoReport
import com.eltavine.duckparse.model.DeviceInfoSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParserScreen(
    viewModel: ParserViewModel,
    sharedImageUri: Uri? = null,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { loadAndParse(viewModel, it, context) }
    }

    // Handle shared image from intent
    LaunchedEffect(sharedImageUri) {
        sharedImageUri?.let { loadAndParse(viewModel, it, context) }
    }

    // Predictive back: when viewing a report, back returns to picker state
    PredictiveBackHandler(enabled = uiState.value.report != null) {
        viewModel.clear()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("DuckParse") },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Pick image button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Screenshot")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clear() },
                    ) {
                        Text("Clear")
                    }
                }
            }

            // Image label
            uiState.value.selectedImageLabel?.let { label ->
                item {
                    Text(
                        text = "Source: $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Loading
            if (uiState.value.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Scanning image...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Error
            uiState.value.errorMessage?.let { error ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // Report with predictive back animation
            uiState.value.report?.let { report ->
                item {
                    AnimatedContent(
                        targetState = report,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 4 }) togetherWith
                                (fadeOut() + slideOutVertically { -it / 4 })
                        },
                    ) { currentReport ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SourceBadge(currentReport.source)
                            DeviceInfoReportCard(currentReport)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(source: String) {
    val (icon, label) = when {
        source.contains("qr") && source.contains("watermark") ->
            Icons.Rounded.CheckCircle to "QR Code + Watermark"
        source.contains("qr") ->
            Icons.Rounded.QrCodeScanner to "QR Code"
        source.contains("watermark") ->
            Icons.Rounded.WaterDrop to "Blind Watermark"
        else -> Icons.Rounded.Image to source
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
fun DeviceInfoReportCard(report: DeviceInfoReport) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        if (report.timestamp.isNotEmpty() || report.appVersion.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (report.appVersion.isNotEmpty()) {
                    Text(
                        text = "v${report.appVersion}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (report.timestamp.isNotEmpty()) {
                    Text(
                        text = report.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Sections
        report.sections.forEach { section ->
            DeviceInfoSectionCard(section)
        }
    }
}

@Composable
fun DeviceInfoSectionCard(section: DeviceInfoSection) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = section.title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            section.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.35f),
                    )
                    Text(
                        text = entry.value,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = if (entry.label == "Fingerprint") FontFamily.Monospace else FontFamily.Default,
                        ),
                        modifier = Modifier.weight(0.65f),
                    )
                }
                if (index < section.entries.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

private fun loadAndParse(
    viewModel: ParserViewModel,
    uri: Uri,
    context: android.content.Context,
) {
    try {
        val resolver = context.contentResolver

        // Sample image dimensions first to avoid OOM
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }

        // Calculate sample size for large images (max 2048px on any side)
        val maxDim = 2048
        val sampleSize = if (opts.outWidth > maxDim || opts.outHeight > maxDim) {
            maxOf(opts.outWidth, opts.outHeight) / maxDim
        } else {
            1
        }

        // Decode at reduced resolution
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }

        if (bitmap != null) {
            val label = uri.lastPathSegment ?: "image"
            viewModel.parseImage(bitmap, label)
        } else {
            android.widget.Toast.makeText(context, "Failed to decode image", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
