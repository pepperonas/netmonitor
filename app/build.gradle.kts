import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pepperonas.netmonitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pepperonas.netmonitor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"
    }

    signingConfigs {
        create("release") {
            val envStoreFile = System.getenv("RELEASE_STORE_FILE")
            if (!envStoreFile.isNullOrEmpty()) {
                storeFile = file(envStoreFile)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            } else {
                val props = rootProject.file("local.properties")
                if (props.exists()) {
                    val localProps = Properties().apply { props.inputStream().use { load(it) } }
                    val sf = localProps.getProperty("RELEASE_STORE_FILE", "")
                    if (sf.isNotEmpty()) {
                        storeFile = file(sf)
                        storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
                        keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
                        keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
