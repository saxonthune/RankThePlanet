# SQLDelight and kotlinx.serialization generate direct references; keep only
# annotation metadata used by serializers and native interop.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# Tink references these compile-time-only Error Prone annotations.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
