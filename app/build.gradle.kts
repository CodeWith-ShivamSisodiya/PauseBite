plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val keyPath = providers.environmentVariable("PAUSEBITE_KEYSTORE_FILE").orNull
val storePass = providers.environmentVariable("PAUSEBITE_KEYSTORE_PASSWORD").orNull
val alias = providers.environmentVariable("PAUSEBITE_KEY_ALIAS").orNull
val keyPass = providers.environmentVariable("PAUSEBITE_KEY_PASSWORD").orNull
val values = listOf(keyPath, storePass, alias, keyPass)
require(values.all { !it.isNullOrBlank() } || values.all { it.isNullOrBlank() }) {
 "Provide all four PauseBite signing variables together"
}
android {
 namespace = "com.pausebite.poc"
 compileSdk = 35
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 defaultConfig { applicationId = "com.pausebite.overlaytest"; minSdk = 26; targetSdk = 35; versionCode = 7; versionName = "0.7-transparency" }
 signingConfigs {
  if (!keyPath.isNullOrBlank()) create("persistent") {
   storeFile = file(keyPath)
   storeType = "PKCS12"
   storePassword = storePass
   keyAlias = alias
   keyPassword = keyPass
  }
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = false
   isDebuggable = false
   if (!keyPath.isNullOrBlank()) signingConfig = signingConfigs.getByName("persistent")
  }
 }
}