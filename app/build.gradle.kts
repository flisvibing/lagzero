plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.lagzero.vpnrouter"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Perform the logo copying or fallback generation synchronously during build configurations
val destDir = file("src/main/res/drawable")
destDir.mkdirs()

// Search for uploaded logo files in the root workspace folder
val candidates = listOf(
  "lagzero.jpeg", "lagzero.jpg", "lagzero.png",
  "lagzero.JPEG", "lagzero.JPG", "lagzero.PNG"
)
var foundFile: File? = null
for (c in candidates) {
  val f = file("${rootDir}/$c")
  if (f.exists() && f.isFile) {
    foundFile = f
    break
  }
}

val targetXmlFile = file("${destDir}/lagzero_logo.xml")
val targetJpgFile = file("${destDir}/lagzero_logo.jpg")
val targetPngFile = file("${destDir}/lagzero_logo.png")

val localFile = foundFile
if (localFile != null) {
  // Clear the XML fallback to avoid duplicate resource collisions
  if (targetXmlFile.exists()) {
    targetXmlFile.delete()
  }
  val ext = localFile.extension.lowercase()
  val finalTarget = if (ext == "png") {
    if (targetJpgFile.exists()) targetJpgFile.delete()
    targetPngFile
  } else {
    if (targetPngFile.exists()) targetPngFile.delete()
    targetJpgFile
  }
  localFile.copyTo(finalTarget, overwrite = true)
  logger.lifecycle("LAGZERO LOGO: Successfully copied ${localFile.name} to ${finalTarget.name}.")
} else {
  // Default modern vector emblem fallback so compilation never breaks
  if (!targetXmlFile.exists() && !targetJpgFile.exists() && !targetPngFile.exists()) {
    val fallbackXml = """
      <vector xmlns:android="http://schemas.android.com/apk/res/android"
          android:width="120dp"
          android:height="120dp"
          android:viewportWidth="120"
          android:viewportHeight="120">
          <path
              android:fillColor="#1E293B"
              android:pathData="M60,10 C90,10 110,25 110,55 C110,85 85,105 60,115 C35,105 10,85 10,55 C10,25 30,10 60,10 Z" />
          <path
              android:fillColor="#29B6F6"
              android:pathData="M60,18 C83,18 98,30 98,54 C98,77 78,94 60,102 C42,94 22,77 22,54 C22,30 37,18 60,18 Z" />
          <path
              android:fillColor="#FFFFFF"
              android:pathData="M65,35 L40,68 L58,68 L50,92 L80,55 L58,55 Z" />
      </vector>
    """.trimIndent()
    targetXmlFile.writeText(fallbackXml)
    logger.lifecycle("LAGZERO LOGO: Created beautiful fallback vector logo.")
  }
}

