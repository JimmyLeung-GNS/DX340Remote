plugins { id("com.android.application"); kotlin("android") }
android {
    namespace = "org.dx340.remote.controller"; compileSdk = 36
    defaultConfig { applicationId = "org.dx340.remote.controller"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
dependencies { implementation(project(":controller-core")) }

apply(from = rootProject.file("development-signing.gradle"))
