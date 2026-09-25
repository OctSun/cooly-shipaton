import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    // Kotlin is already on the classpath via the KMP plugin, so apply without a version.
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.composeCompiler)
}

// Read the Google Maps key from local.properties so it's never committed.
val mapsApiKey: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("MAPS_API_KEY", "")

// Release signing from keystore.properties (git-ignored); absent on machines that only build debug.
val keystoreProps: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.plain)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.octsun.cooly"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.octsun.cooly"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "1.2.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Fail loudly instead of silently shipping a gray map: a release AAB with no
            // MAPS_API_KEY renders blank Google tiles on every device.
            // Scoped to THIS module's release tasks only — the iOS framework build
            // (:shared:linkReleaseFramework...) also has "Release" in task names and must
            // not trip this guard on CI where local.properties is absent.
            if (mapsApiKey.isBlank()) {
                gradle.taskGraph.whenReady {
                    val androidRelease = allTasks.any {
                        it.project.name == "androidApp" && it.name.contains("Release", ignoreCase = true)
                    }
                    if (androidRelease) {
                        throw GradleException(
                            "MAPS_API_KEY is missing from local.properties — a release build " +
                                "would ship a blank/gray map. Add MAPS_API_KEY=... before bundling.",
                        )
                    }
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        // False positive: we use ComponentActivity (not Fragment) for registerForActivityResult.
        disable += "InvalidFragmentVersionForActivityResult"
        // AGP 8.13's bundled lint can't read Kotlin 2.4.10 metadata; skip lint on release builds.
        checkReleaseBuilds = false
    }
}