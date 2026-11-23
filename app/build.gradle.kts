plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.pausebite.poc"
 compileSdk = 35
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 defaultConfig { applicationId = "com.pausebite.overlaytest"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3-auto-detection" }
}