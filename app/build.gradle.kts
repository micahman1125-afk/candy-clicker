plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

import java.util.zip.ZipFile

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.candyclicker.nwqypt"
    minSdk = 24
    targetSdk = 35
    versionCode = 11
    versionName = "2.1"

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
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
  lint {
    disable.add("AndroidGradlePluginVersion")
    disable.add("GradleDependency")
    disable.add("NewerVersionAvailable")
  }
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
  // implementation(platform(libs.firebase.bom))
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
  // implementation("androidx.compose.ui:ui-text-google-fonts")
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
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

abstract class CopyApkTask : DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val apkDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.Internal
    abstract val rootDirProp: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun copyApk() {
        val src = apkDir.file("app-debug.apk").get().asFile
        if (src.exists()) {
            val rootDir = rootDirProp.get().asFile
            
            // Copy to root workspace
            val destRoot = File(rootDir, "app-debug.apk")
            src.copyTo(destRoot, overwrite = true)
            println("Successfully copied APK to root workspace: ${destRoot.absolutePath} (Size: ${destRoot.length()} bytes)")

            // Inspect zip entries of the APK to find large files
            try {
                ZipFile(src).use { zip ->
                    val entries = zip.entries().asSequence().toList()
                    println("=== Top 15 Largest Files inside APK ===")
                    entries.sortedByDescending { it.size }
                        .take(15)
                        .forEach { entry ->
                            println("${entry.name}: size = ${entry.size} bytes, compressed = ${entry.compressedSize} bytes")
                        }
                }
            } catch (e: Exception) {
                println("Failed to read zip entries: ${e.message}")
            }

            // List sizes of image files in res/drawable and res/drawable-nodpi
            val drawableDir = File(rootDir, "app/src/main/res/drawable")
            val drawableNodpiDir = File(rootDir, "app/src/main/res/drawable-nodpi")
            println("=== File Sizes in res/drawable ===")
            drawableDir.listFiles()?.forEach { file ->
                println("${file.name}: ${file.length()} bytes")
            }
            println("=== File Sizes in res/drawable-nodpi ===")
            drawableNodpiDir.listFiles()?.forEach { file ->
                println("${file.name}: ${file.length()} bytes")
            }

            // Copy to .build-outputs/app-debug.apk
            val destBuild = File(rootDir, ".build-outputs/app-debug.apk")
            destBuild.parentFile?.mkdirs()
            src.copyTo(destBuild, overwrite = true)
            println("Successfully copied APK to .build-outputs: ${destBuild.absolutePath} (Size: ${destBuild.length()} bytes)")
        }
    }
}

tasks.register<CopyApkTask>("copyApkToRootTask") {
    apkDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
    rootDirProp.set(project.rootProject.layout.projectDirectory)
}

tasks.whenTaskAdded {
    if (name == "assembleDebug") {
        finalizedBy("copyApkToRootTask")
    }
}

