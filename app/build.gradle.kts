plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.pausebite.poc"
 compileSdk = 35
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 defaultConfig { applicationId = "com.pausebite.overlaytest"; minSdk = 26; targetSdk = 35; versionCode = 4; versionName = "0.4-reliability" }
 signingConfigs {
  create("persistent") {
   val filePath = System.getenv("PAUSEBITE_KEYSTORE_FILE")
   if (!filePath.isNullOrBlank()) {
    storeFile = file(filePath)
    storePassword = System.getenv("PAUSEBITE_KEYSTORE_PASSWORD")
    keyAlias = System.getenv("PAUSEBITE_KEY_ALIAS")
    keyPassword = System.getenv("PAUSEBITE_KEY_PASSWORD")
   }
  }
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = false
   signingConfig = signingConfigs.getByName("persistent")
  }
 }
}