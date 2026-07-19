# TFLite rules
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Ktor rules
-keepattributes *Annotation*, InnerClasses
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

# AndroidX Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**
