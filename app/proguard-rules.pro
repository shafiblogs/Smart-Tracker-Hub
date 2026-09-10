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
# Kotlin properties. CONFIRMED IN PRODUCTION (crash log): keeping only <fields> + <init>()
# is NOT enough — Firestore's POJO mapper is Java-Bean-based and discovers properties via
# getter methods, not raw field access, so R8 stripping the Kotlin-generated getters threw
# "RuntimeException: No properties to serialize found on class <obfuscated name>" the first
# time HomeScreenViewModel.refreshAccountMonth() called doc.toObject(AccountSummary::class.java)
# — a hard crash, not the silent-zero fallback originally assumed here. Keep the whole class
# for both — they're small POJOs, no obfuscation value worth the risk.
-keep class com.marsa.smarttrackerhub.domain.AccountSummary { *; }
-keep class com.marsa.smarttrackerhub.domain.MonthlySummary { *; }

# A transitive dependency references slf4j's optional logging backend binding, which this
# app never bundles — slf4j's own documented design is to fall back to a no-op logger when
# it's absent, so this is safe to suppress rather than keep. AGP's own generated
# missing_rules.txt after the first minified build recommended this exact line.
-dontwarn org.slf4j.impl.StaticLoggerBinder