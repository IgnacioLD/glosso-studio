# Add any project specific keep rules here:

# ONNX Runtime - native JNI code looks up these classes by exact name
-keep class ai.onnxruntime.** { *; }

# F-Droid Reproducible Build Fixes
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-dontnote kotlinx.coroutines.internal.MainDispatcherFactory
-dontwarn org.slf4j.**
