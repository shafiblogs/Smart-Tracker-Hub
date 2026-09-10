# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WorkManager instantiates Worker subclasses via reflection by class name at runtime —
# R8 must not rename/strip them or their (Context, WorkerParameters) constructor, or
# WorkManager crashes at runtime the first time it tries to run one.
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Firestore's .toObject() reflects into these two classes to match document fields to
# Kotlin properties via a no-arg constructor. Unlike a missing Worker, a broken keep rule
# here doesn't crash — toObject() silently returns null/default values for renamed fields,
# so a stripped field would just show up as zero on the dashboard with no error at all.
-keepclassmembers class com.marsa.smarttrackerhub.domain.AccountSummary {
    <fields>;
    <init>();
}
-keepclassmembers class com.marsa.smarttrackerhub.domain.MonthlySummary {
    <fields>;
    <init>();
}

# A transitive dependency references slf4j's optional logging backend binding, which this
# app never bundles — slf4j's own documented design is to fall back to a no-op logger when
# it's absent, so this is safe to suppress rather than keep. AGP's own generated
# missing_rules.txt after the first minified build recommended this exact line.
-dontwarn org.slf4j.impl.StaticLoggerBinder