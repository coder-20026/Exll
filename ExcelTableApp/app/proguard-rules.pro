# ProGuard Rules for WhtsTable Master
# Premium Android App - Release Build Optimization

# Keep application classes
-keep class com.whtstable.master.** { *; }

# Keep DataManager inner classes
-keepclassmembers class com.whtstable.master.DataManager$RowData { *; }

# Keep JSON classes
-keep class org.json.** { *; }

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Material Design Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep onClick methods
-keepclassmembers class * {
    public void onClick(android.view.View);
}

# Keep Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
