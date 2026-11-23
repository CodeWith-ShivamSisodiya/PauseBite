plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.pausebite.poc"
 compileSdk = 35
 defaultConfig { applicationId = "com.pausebite.poc"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1" }
}