# Keep all application code from R8 obfuscation
-keep class com.example.carwash.** { *; }
-keepclassmembers class com.example.carwash.** { *; }
-keep class package.com.example.carwash.** { *; }
-keepclassmembers class package.com.example.carwash.** { *; }

# Keep Firebase Models & Firestore Deserialization
-keep class com.google.firebase.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.Exclude *;
    @com.google.firebase.firestore.IgnoreExtraProperties *;
}

# Keep Hilt Dependency Injection
-keep class * extends androidx.lifecycle.ViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep ML Kit Text Recognition
-keep class com.google.mlkit.vision.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }

# Keep Kotlin Attributes & Annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
