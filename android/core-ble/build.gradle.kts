plugins { id("com.android.library"); kotlin("android") }
android { namespace = "org.dx340.remote.ble"; compileSdk = 36; defaultConfig { minSdk = 31 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 } }
dependencies { implementation(project(":core-protocol")) }
