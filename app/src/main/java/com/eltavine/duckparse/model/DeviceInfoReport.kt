package com.eltavine.duckparse.model

data class DeviceInfoEntry(
    val label: String,
    val value: String,
)

data class DeviceInfoSection(
    val title: String,
    val entries: List<DeviceInfoEntry>,
)

data class DeviceInfoReport(
    val timestamp: String = "",
    val appVersion: String = "",
    val sections: List<DeviceInfoSection> = emptyList(),
    val source: String = "", // "qr" | "watermark" | "qr+watermark"
    val rawQrText: String = "",
    val rawWatermarkLines: List<String> = emptyList(),
)
