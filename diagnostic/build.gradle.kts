plugins { id("com.android.application") }

val keyPath = providers.environmentVariable("PAUSEBITE_KEYSTORE_FILE").orNull
val storePass = providers.environmentVariable("PAUSEBITE_KEYSTORE_PASSWORD").orNull
val alias = providers.environmentVariable("PAUSEBITE_KEY_ALIAS").orNull
val keyPass = providers.environmentVariable("PAUSEBITE_KEY_PASSWORD").orNull
android {
 namespace = "com.pausebite.permissionbaseline"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.pausebite.permissionbaseline"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "1.0-baseline"
 }
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
