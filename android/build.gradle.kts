plugins {
    alias(libs.plugins.android.application) apply false
    // org.jetbrains.kotlin.android is no longer needed/allowed as of AGP 9 -
    // Kotlin compilation is built into com.android.application now.
    alias(libs.plugins.kotlin.compose) apply false
}
