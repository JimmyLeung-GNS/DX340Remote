plugins { id("com.android.application"); kotlin("android") }
android {
    namespace = "org.dx340.remote.host"; compileSdk = 36
    defaultConfig { applicationId = "org.dx340.remote.host"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "0.1.0"; testInstrumentationRunner = "org.dx340.remote.host.SourceProbeInstrumentation" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
dependencies { implementation(project(":core-protocol")); implementation(project(":core-ble")); implementation(project(":source-adapters")); implementation(project(":library-cache")) }
dependencies { androidTestImplementation("androidx.media3:media3-session:1.8.0") }
