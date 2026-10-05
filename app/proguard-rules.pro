# R8 / ProGuard Configuration for Carwash App

# Keep Data & Entity Models used by Firebase/Firestore/JSON Serialization
-keep class com.example.carwash.model.** { *; }
-keepclassmembers class com.example.carwash.model.** { *; }
-keep class com.example.carwash.add.Sale { *; }
-keep class com.example.carwash.add.SaleDraft { *; }

# Keep Firebase Models & Firestore Deserialization
-keep class com.google.firebase.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.Exclude *;
    @com.google.firebase.firestore.IgnoreExtraProperties *;
}

# Keep Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    *** Companion;
}

# Keep Hilt Dependency Injection & ViewModels
-keep class * extends androidx.lifecycle.ViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep ML Kit Text Recognition
-keep class com.google.mlkit.vision.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }
