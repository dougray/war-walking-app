# Add project specific ProGuard rules here.

# Gson deserializes these by field name via reflection - keep them whole so
# R8 doesn't rename/strip fields and silently break WiGLE API parsing.
-keep class com.warwalking.app.network.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retain generic signatures of TypeToken and its subclasses (Gson's own
# recommended rule for R8 3.0+).
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Room entities/DAOs - annotation-processed at compile time, keep them intact.
-keep class com.warwalking.app.data.** { *; }
