# Pleos Vehicle SDK: 메서드 주석(@RequiresCustomPermission)을 리플렉션으로 읽는다
-keep class ai.pleos.** { *; }
-keep interface ai.pleos.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-dontwarn ai.pleos.**
