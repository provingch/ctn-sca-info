import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

// Firebase (FCM) is optional: only wire the Google Services plugin when the
// developer has dropped in app/google-services.json. Without it the app still
// builds and runs, just without push notifications.
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Base URL of the SCA backend. Override per build with:
//   ./gradlew assembleRelease -PscaBaseUrl=https://otro-host/
// or by setting sca.baseUrl in android/local.properties (gitignored).
val scaBaseUrl: String = (project.findProperty("scaBaseUrl") as String?)
    ?: runCatching {
        Properties().apply {
            rootProject.file("local.properties").inputStream().use { load(it) }
        }.getProperty("sca.baseUrl")
    }.getOrNull()
    ?: "https://ctn-sca.ddns.net/"

android {
    namespace = "py.edu.ctn.sca.padres"
    compileSdk = 36

    defaultConfig {
        applicationId = "py.edu.ctn.sca.padres"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SCA_BASE_URL", "\"$scaBaseUrl\"")
    }

    signingConfigs {
        // Sideload signing. Point these at a keystore you generate once with:
        //   keytool -genkeypair -v -keystore sca-padres.jks -alias sca -keyalg RSA -keysize 2048 -validity 10000
        // then set the values in android/keystore.properties (gitignored).
        create("release") {
            val props = Properties()
            val f = rootProject.file("keystore.properties")
            if (f.exists()) {
                f.inputStream().use { props.load(it) }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val ks = rootProject.file("keystore.properties")
            if (ks.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
