plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.mrunicorn.sb"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.mrunicorn.sb"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }

  // Signing configuration: reads from gradle.properties when available.
  signingConfigs {
    create("release") {
      val storeFileProp = project.findProperty("RELEASE_STORE_FILE") as String?
      if (!storeFileProp.isNullOrBlank()) {
        storeFile = file(storeFileProp)
        storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as String?) ?: ""
        keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?) ?: ""
        keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as String?) ?: ""
      } else {
        // Fallback for portability: use the local debug keystore if present.
        // This is suitable for testing on a new machine but NOT for production.
        val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
        if (debugKeystore.exists()) {
          storeFile = debugKeystore
          storePassword = "android"
          keyAlias = "androiddebugkey"
          keyPassword = "android"
        }
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
  // Use the signing config named "release" (defined below).
  // To provide a real release keystore, set these properties in
  // `gradle.properties` (do NOT commit secrets):
  // RELEASE_STORE_FILE=/absolute/path/to/keystore.jks
  // RELEASE_STORE_PASSWORD=your_store_password
  // RELEASE_KEY_ALIAS=your_key_alias
  // RELEASE_KEY_PASSWORD=your_key_password
  signingConfig = signingConfigs.getByName("release")
    }
    debug {
      applicationIdSuffix = ".debug"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }

  buildFeatures { compose = true }

  // NOTE: Do NOT include composeOptions{} when using Kotlin 2.0 + compose plugin
  packaging { resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}") }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.3.0-beta01")
  implementation("androidx.compose.material:material-icons-extended")
  debugImplementation("androidx.compose.ui:ui-tooling")

  // Room
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // Optional image previews
  implementation("io.coil-kt:coil-compose:2.7.0")

  // HTML parsing
  implementation("org.jsoup:jsoup:1.17.2")

  // Testing
  testImplementation("junit:junit:4.13.2")
}
