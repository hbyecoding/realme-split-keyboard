plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
dependencies { testImplementation("junit:junit:4.13.2") }
android {
    namespace = "dev.splitkeyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.splitkeyboard"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
