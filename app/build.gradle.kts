
plugins {
    id("dev.trickuild")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


val appVersionName = "2.2.0-IntDev_rev3"

trickuild {
    codename.set("Epica")
    buildNumber {
        enabled.set(true)
        hashLength.set(5)
        timestampSource.set("build")
        timeZone.set("Asia/Tokyo")
    }
    roulette {
        enabled.set(true)
        namespace.set("GlyphBarty")
    }
}

android {
    namespace = "jp.linkserver.glyphvisualizer"

    compileSdk = 37

    defaultConfig {
        applicationId = "jp.linkserver.glyphvisualizer"
        minSdk = 33
        targetSdk = 36
        versionCode = 18
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            isMinifyEnabled = false
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
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
    androidResources {
        localeFilters += listOf("en", "ja")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    implementation("com.github.Link2011-Act2.Goldy:elief:809dda97ab45cd28812f150697cba126c6d4d406")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("com.github.Link2011-Act2:Pyrite:508b1cf5131679fee7581acbc7ea55a365b6ff2c")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
