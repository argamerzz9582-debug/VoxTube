# SPDX-FileCopyrightText: 2025 Flow
# SPDX-License-Identifier: GPL-3.0-or-later
# Based on NewPipe's ProGuard configuration

# https://developer.android.com/build/shrink-code

## Open-source readable stack traces (no de-obfuscation pipeline required)
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

## Strip debug and verbose logging from release binaries
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

-keepattributes Exceptions, InnerClasses

## Rules for Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

## Keep data models and serialization structures
-keep class com.voxtube.app.data.model.** { *; }
-keep class com.voxtube.app.data.local.** { *; }
-keep class com.voxtube.app.data.lyrics.** { *; }

## Gson-persisted models that live OUTSIDE the packages above (issue #996): without an
## explicit keep, R8 may strip the generic Signature of MusicTrack.artists, and cached
## history/queues then deserialize artist entries as LinkedTreeMaps that crash the music
## feed with a ClassCastException on first access. Any new Gson-persisted model must be
## added here (or live in a kept package), whatever package it renders from.
-keep class com.voxtube.app.data.music.model.MusicTrack { *; }
-keep class com.voxtube.app.data.music.model.MusicArtist { *; }
-keep class com.voxtube.app.data.music.model.MusicItemType { *; }
-keep class com.voxtube.app.data.music.Playlist { *; }
-keep class com.voxtube.app.data.music.DownloadedTrack { *; }

## Shazam recognition models + kotlinx serializers
-keepclasseswithmembers class com.voxtube.app.data.recognition.shazam.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.voxtube.app.data.recognition.shazam.** {
    *** Companion;
}

## Rules for Ktor
-dontwarn io.ktor.**

## Rules for Brotli
-dontwarn org.brotli.**
-dontwarn org.conscrypt.**

## Third-party / Platform warning suppressions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**
-dontwarn java.beans.**