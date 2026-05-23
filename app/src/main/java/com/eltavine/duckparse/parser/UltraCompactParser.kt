package com.eltavine.duckparse.parser

import com.eltavine.duckparse.model.DeviceInfoEntry
import com.eltavine.duckparse.model.DeviceInfoReport
import com.eltavine.duckparse.model.DeviceInfoSection

object UltraCompactParser {

    private val keyMap = mapOf(
        // Identity
        "br" to ("Identity" to "Brand"),
        "mfr" to ("Identity" to "Manufacturer"),
        "mo" to ("Identity" to "Model"),
        "de" to ("Identity" to "Device"),
        "pr" to ("Identity" to "Product"),
        "bd" to ("Identity" to "Board"),
        // SOC
        "sm" to ("SOC / Chipset" to "SOC Manufacturer"),
        "sc" to ("SOC / Chipset" to "SOC Model"),
        "ch" to ("SOC / Chipset" to "CPU Hardware"),
        "bp" to ("SOC / Chipset" to "Board Platform"),
        "cn" to ("SOC / Chipset" to "Chip Name"),
        "co" to ("SOC / Chipset" to "CPU Cores"),
        "ca" to ("SOC / Chipset" to "CPU Architecture"),
        // Build
        "hw" to ("Build" to "Hardware"),
        "bl" to ("Build" to "Bootloader"),
        "fp" to ("Build" to "Fingerprint"),
        "bi" to ("Build" to "Build ID"),
        "bt" to ("Build" to "Build type"),
        "in" to ("Build" to "Incremental"),
        // Android
        "sk" to ("Android" to "SDK"),
        "re" to ("Android" to "Release"),
        "sp" to ("Android" to "Security patch"),
        "tg" to ("Android" to "Tags"),
        "bu" to ("Android" to "Build user"),
        "bh" to ("Android" to "Build host"),
        // Runtime
        "ab" to ("Runtime" to "Primary ABI"),
        "kn" to ("Runtime" to "Kernel"),
        "lo" to ("Runtime" to "Locale"),
        "tz" to ("Runtime" to "Time zone"),
    )

    fun parse(raw: String): DeviceInfoReport {
        val text = raw.removePrefix("DD|").trim()
        val tokens = text.split("|").map { it.trim() }

        val pairs = mutableMapOf<String, String>()
        for (token in tokens) {
            val eq = token.indexOf('=')
            if (eq > 0) {
                val key = token.substring(0, eq).trim()
                val value = token.substring(eq + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    pairs[key] = value
                }
            }
        }

        val timestamp = pairs.remove("t") ?: ""
        val appVersion = pairs.remove("av") ?: ""

        val sectionMap = linkedMapOf<String, MutableList<DeviceInfoEntry>>()
        for ((key, value) in pairs) {
            val (sectionName, label) = keyMap[key] ?: ("Other" to key)
            sectionMap.getOrPut(sectionName) { mutableListOf() }
                .add(DeviceInfoEntry(label, value))
        }

        val sectionOrder = listOf(
            "Identity", "SOC / Chipset", "Build", "Android", "Runtime",
        )
        val sections = mutableListOf<DeviceInfoSection>()
        for (name in sectionOrder) {
            sectionMap.remove(name)?.let { entries ->
                sections.add(DeviceInfoSection(name, entries))
            }
        }
        sectionMap.forEach { (title, entries) ->
            sections.add(DeviceInfoSection(title, entries))
        }

        return DeviceInfoReport(
            timestamp = timestamp,
            appVersion = appVersion,
            sections = sections,
            source = "qr",
            rawQrText = raw,
        )
    }

    fun mergeWatermark(report: DeviceInfoReport, watermarkLines: List<String>): DeviceInfoReport {
        if (watermarkLines.isEmpty()) return report

        val newSource = if (report.source == "qr") "qr+watermark" else "watermark"
        val wmSection = DeviceInfoSection(
            title = "Watermark (OCR)",
            entries = watermarkLines.map { DeviceInfoEntry("Line", it) },
        )
        return report.copy(
            source = newSource,
            sections = report.sections + wmSection,
            rawWatermarkLines = watermarkLines,
        )
    }
}
