# Iron Fury ProGuard Rules

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep game classes (SharedPreferences keys use class/field names)
-keep class com.contra.game.weapons.WeaponType { *; }
-keep class com.contra.game.utils.ControlSize { *; }

# AndroidX
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
