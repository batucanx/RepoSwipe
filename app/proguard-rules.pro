# Add project specific ProGuard rules here.
#
# Retrofit, OkHttp, Room, Hilt, WorkManager, Coil, and Firebase Firestore all ship their own
# consumer ProGuard rules inside their AARs, so none of that is repeated here. The one thing
# worth keeping explicitly is kotlinx.serialization's generated serializers for our own
# @Serializable DTOs (core:network/.../model, feature:auth/.../data) — this is the rule
# kotlinx.serialization's own docs recommend apps add on top of its consumer rules, since the
# library's generic rule can't know our package names ahead of time.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.batuhan.reposwipe.**$$serializer { *; }
-keepclassmembers class com.batuhan.reposwipe.** {
    *** Companion;
}
-keepclasseswithmembers class com.batuhan.reposwipe.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Sentry's own classes are covered by its consumer rules; note that *our* app code's stack
# traces will still show obfuscated names in Sentry unless mapping.txt is uploaded to Sentry
# (via the Sentry Gradle plugin, not currently applied — see CLAUDE.md / README before adding it,
# it needs a SENTRY_AUTH_TOKEN).
