# Add project specific ProGuard rules here.
# Keep Kotlin data classes used with Gson/Firestore serialization
-keepclassmembers class com.perenniasystems.lascito.data.model.** { *; }
