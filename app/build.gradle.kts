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

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
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
  val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.3.0")
  implementation("androidx.compose.material:material-icons-extended")
  debugImplementation("androidx.compose.ui:ui-tooling")

  // Room
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // Optional image previews
  implementation("io.coil-kt:coil-compose:2.7.0")
}
