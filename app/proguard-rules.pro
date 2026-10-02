# Keep model classes serialized for Firestore & JSON
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
}
-keepclassmembers class package.com.example.carwash.model.** { *; }
-keepclassmembers class package.com.example.carwash.add.** { *; }

# Keep Firebase Models
-keep class com.google.firebase.** { *; }

# Keep ML Kit Text Recognition
-keep class com.google.mlkit.vision.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }
