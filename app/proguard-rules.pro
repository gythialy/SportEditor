# Xposed entry point is referenced from META-INF/xposed/java_init.list by name only.
-keep class io.github.sporteditor.hook.SportEditorModule { *; }
-keep,allowoptimization class * extends io.github.libxposed.api.XposedModule { <init>(); }

# Keep settings keys and enum entry names used across the module/target process boundary.
-keepclassmembers enum io.github.sporteditor.data.** { *; }

# AndroidX / Compose ship their own consumer rules.
-dontwarn io.github.libxposed.api.**
