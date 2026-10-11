import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Optional stable signing key (e.g. provided by CI secrets). Without it, the default debug key is used,
// which differs between machines/CI runs – an APK signed with another key can't be installed as an update.
val signingKeystore = System.getenv("SIGNING_KEYSTORE")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "de.habits.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.habits.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        if (signingKeystore != null) {
            create("stable") {
                storeFile = signingKeystore
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                // PKCS12 keystores (keytool default) have a single password, so the key password may be omitted.
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")?.takeIf { it.isNotEmpty() } ?: storePassword
            }
        }
    }

    buildTypes {
        // Own package + name, so a local dev build installs next to the CI release (different signing key)
        // and never touches its data. Label override: src/debug/res/values/strings.xml
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("stable") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    // Previews and their sample data live in src/debug and never ship in release builds.
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
