plugins { id("com.android.application") }
android { namespace = "ru.decomposer.app"; compileSdk = 35
 defaultConfig { applicationId = "ru.decomposer.app"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
