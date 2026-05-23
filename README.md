# DuckParse

Android app that parses Duck Detector screenshots — decodes **QR codes** and extracts **blind watermarks** to display device information in a clean UI.

## Features

- 📷 Select or share a Duck Detector screenshot
- 📱 Decode ultra-compact QR codes (ML Kit Barcode Scanning)
- 🔍 Extract blind watermark text (ML Kit Text Recognition OCR)
- 📊 Display parsed device info in structured cards
- 🎨 Material 3 UI with dark/light theme

## Build

```bash
./gradlew assembleRelease
```

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| QR Decode | Google ML Kit Barcode Scanning |
| OCR | Google ML Kit Text Recognition |
| Image Load | Coil |
