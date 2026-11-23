plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.pausebite.probe"
 compileSdk = 35
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 defaultConfig { minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "probe-1" }
 flavorDimensions += "component"
 productFlavors {
  create("overlayCode") { dimension = "component"; applicationId = "com.pausebite.probe.overlaycode" }
  create("monitorCode") { dimension = "component"; applicationId = "com.pausebite.probe.monitorcode" }
  create("combinedCode") { dimension = "component"; applicationId = "com.pausebite.probe.combinedcode" }
 }
 buildTypes { getByName("release") { isMinifyEnabled = false; isDebuggable = false } }
}
