plugins { id("com.android.application") }
android {
 namespace = "com.pausebite.cleancontrol"
 compileSdk = 35
 defaultConfig { minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "control-1" }
 flavorDimensions += "test"
 productFlavors {
  create("minimal") { dimension = "test"; applicationId = "com.pausebite.control.minimal" }
  create("overlay") { dimension = "test"; applicationId = "com.pausebite.control.overlay" }
 }
 buildTypes { getByName("release") { isMinifyEnabled = false; isDebuggable = false } }
}
