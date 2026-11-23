# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class ch.awakened.app.** {
    *** Companion;
}
-keepclasseswithmembers class ch.awakened.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ch.awakened.app.**$$serializer { *; }
