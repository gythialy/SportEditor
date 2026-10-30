import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing lives outside the repository, by default in ~/.vault.
// CI can override every value through environment variables (see docs/release.md).
val vaultDir = file(
    System.getenv("SPORTEDITOR_VAULT_DIR") ?: "${System.getProperty("user.home")}/.vault"
)
val signingPropertiesFile = vaultDir.resolve("sporteditor-signing.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.inputStream().use { load(it) }
    }
}
val signingValue: (String, String) -> String? = { envName, key ->
    System.getenv(envName) ?: signingProperties.getProperty(key)
}
val releaseStoreFile = signingValue("RELEASE_KEYSTORE_PATH", "storeFile")?.let { raw ->
    val candidate = file(raw)
    if (candidate.isAbsolute) candidate else vaultDir.resolve(raw)
}
val releaseStorePassword = signingValue("RELEASE_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = releaseStoreFile?.isFile == true &&
    !releaseStorePassword.isNullOrEmpty() &&
    !releaseKeyAlias.isNullOrEmpty() &&
    !releaseKeyPassword.isNullOrEmpty()

val buildingRelease = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}
if (buildingRelease && System.getenv("REQUIRE_RELEASE_SIGNING") == "true" && !hasReleaseSigning) {
    error(
        "Release signing is not configured. Set RELEASE_KEYSTORE_PATH, " +
            "RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS and RELEASE_KEY_PASSWORD."
    )
}

android {
    namespace = "io.github.sporteditor"
    compileSdk = 37
    compileSdkMinor = 1
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.github.sporteditor"
        minSdk = 26
        targetSdk = 37
        versionCode = 106
        versionName = "1.4.1"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            // Local development and the CI artifact build fall back to the debug key.
            // The release workflow sets REQUIRE_RELEASE_SIGNING=true and fails early
            // when the release keystore is missing, so a real release is never debug-signed.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    // Provided by the Xposed framework at runtime, never packaged.
    compileOnly(libs.libxposed.api)
    // Shipped in the module APK: Xposed service client + XposedProvider.
    implementation(libs.libxposed.service)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
