plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val keyPath = providers.environmentVariable("PAUSEBITE_KEYSTORE_FILE").orNull
val storePass = providers.environmentVariable("PAUSEBITE_KEYSTORE_PASSWORD").orNull
val alias = providers.environmentVariable("PAUSEBITE_KEY_ALIAS").orNull
val keyPass = providers.environmentVariable("PAUSEBITE_KEY_PASSWORD").orNull

android {
 namespace = "com.pausebite.poc"
 compileSdk = 35
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 defaultConfig {
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "isolation-1"
 }
 flavorDimensions += "capability"
 productFlavors {
  create("uiOnly") { dimension = "capability"; applicationId = "com.pausebite.isolation.uionly" }
  create("uiPermissions") { dimension = "capability"; applicationId = "com.pausebite.isolation.uipermissions" }
  create("full") { dimension = "capability"; applicationId = "com.pausebite.isolation.full" }
  create("onlyOverlay") { dimension = "capability"; applicationId = "com.pausebite.isolation.onlyoverlay" }
  create("onlyUsage") { dimension = "capability"; applicationId = "com.pausebite.isolation.onlyusage" }
  create("onlyForeground") { dimension = "capability"; applicationId = "com.pausebite.isolation.onlyforeground" }
  create("onlySpecialUse") { dimension = "capability"; applicationId = "com.pausebite.isolation.onlyspecialuse" }
  create("onlyNotifications") { dimension = "capability"; applicationId = "com.pausebite.isolation.onlynotifications" }
 }
 sourceSets.getByName("main").java.srcDir("../app/src/main/java")
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
