package com.eltavine.duckparse.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eltavine.duckparse.model.DeviceInfoReport
import com.eltavine.duckparse.parser.QrDecoder
import com.eltavine.duckparse.parser.UltraCompactParser
import com.eltavine.duckparse.parser.WatermarkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParserUiState(
    val isLoading: Boolean = false,
    val report: DeviceInfoReport? = null,
    val errorMessage: String? = null,
    val selectedImageLabel: String? = null,
)

class ParserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ParserUiState())
    val uiState: StateFlow<ParserUiState> = _uiState.asStateFlow()

    fun parseImage(bitmap: Bitmap, label: String, qrOnly: Boolean = false) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = ParserUiState(
                isLoading = true,
                selectedImageLabel = label,
            )

            try {
                var report: DeviceInfoReport? = null

                // 1. QR code (always)
                val qrTexts = QrDecoder.decode(bitmap)
                if (qrTexts.isNotEmpty()) {
                    report = UltraCompactParser.parse(qrTexts.first())
                }

                // 2. Watermark (skip in QR-only mode)
                if (!qrOnly) {
                    val wmLines = WatermarkExtractor.extract(bitmap)
                    if (wmLines.isNotEmpty()) {
                        report = if (report != null) {
                            UltraCompactParser.mergeWatermark(report, wmLines)
                        } else {
                            DeviceInfoReport(
                                source = "watermark",
                                sections = listOf(
                                    com.eltavine.duckparse.model.DeviceInfoSection(
                                        title = "Watermark (OCR)",
                                        entries = wmLines.map {
                                            com.eltavine.duckparse.model.DeviceInfoEntry("Line", it)
                                        },
                                    ),
                                ),
                                rawWatermarkLines = wmLines,
                            )
                        }
                    }
                }

                if (report != null) {
                    _uiState.value = ParserUiState(
                        report = report,
                        selectedImageLabel = label,
                    )
                } else {
                    _uiState.value = ParserUiState(
                        errorMessage = "No Duck Detector data found in this image.",
                        selectedImageLabel = label,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ParserUiState(
                    errorMessage = "Parse error: ${e.message}",
                    selectedImageLabel = label,
                )
            }
        }
    }

    fun clear() {
        _uiState.value = ParserUiState()
    }
}
