plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.protobuf")
}

android {
    namespace = "dev.typezero.couchlink.remote"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.typezero.couchlink.remote"
        minSdk = 28
        targetSdk = 36
        versionCode = 115
        versionName = "1.2-dev.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        val keystoreFile = providers.environmentVariable("COUCHLINK_KEYSTORE_FILE").orNull
        val keystorePassword = providers.environmentVariable("COUCHLINK_KEYSTORE_PASSWORD").orNull
        val keyAliasValue = providers.environmentVariable("COUCHLINK_KEY_ALIAS").orNull
        val keyPasswordValue = providers.environmentVariable("COUCHLINK_KEY_PASSWORD").orNull

        if (!keystoreFile.isNullOrBlank() &&
            !keystorePassword.isNullOrBlank() &&
            !keyAliasValue.isNullOrBlank() &&
            !keyPasswordValue.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        )
        jniLibs.keepDebugSymbols += "**/libandroidx.graphics.path.so"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.protobuf:protobuf-javalite:4.31.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.31.1" }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") { option("lite") }
            }
        }
    }
}
