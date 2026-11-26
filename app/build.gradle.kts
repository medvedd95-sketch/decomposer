plugins { id("com.android.application") }
android { namespace = "ru.decomposer.app"; compileSdk = 35
 defaultConfig { applicationId = "ru.decomposer.app"; minSdk = 26; targetSdk = 35; versionCode = 7; versionName = "0.7" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
