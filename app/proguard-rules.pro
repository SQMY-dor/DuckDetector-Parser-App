# DuckParse ProGuard / R8 rules

# --- ML Kit Barcode Scanning ---
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.barcode.common.** { *; }
-dontwarn com.google.mlkit.vision.barcode.**

# --- ML Kit Text Recognition ---
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.text.chinese.** { *; }
-dontwarn com.google.mlkit.vision.text.**

# --- ML Kit common ---
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.common.** { *; }
-dontwarn com.google.mlkit.**

# --- ZXing ---
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# --- Compose ---
-dontwarn androidx.compose.**
