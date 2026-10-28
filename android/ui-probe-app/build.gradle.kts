plugins { id("com.android.application"); kotlin("android") }
android {
    namespace = "org.dx340.remote.probe"; compileSdk = 36
    defaultConfig { applicationId = "org.dx340.remote.probe"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
