plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "org.dx340.remote.controller.core"; compileSdk = 36
    defaultConfig { minSdk = 31 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
dependencies { implementation(project(":core-protocol")); implementation(project(":core-ble")) }

dependencies { testImplementation("junit:junit:4.13.2"); testImplementation("org.json:json:20180813") }
