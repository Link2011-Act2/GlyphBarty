import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val buildNumberFiles = (
    fileTree("src") {
        exclude("**/build/**")
    }.files + listOf(
        project.file("build.gradle.kts"),
        rootProject.file("build.gradle.kts"),
        rootProject.file("settings.gradle.kts"),
        rootProject.file("gradle/libs.versions.toml")
    ).filter { it.isFile }
).sortedBy { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath }

val appCodeName = "Epica" // 0.x.0のxが変わるたびに更新したいね トリッカルの使徒から取る
val appVersionName = "2.2.0-IntDev_rev0"

// Keep Major.Minor as text: 2.10 must never become the decimal value 2.1.
fun rollVersionPersonality(versionName: String): Pair<String, Int> {
    val version = Regex(
        """(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:\.(?:0|[1-9][0-9]*))?(?:-[A-Za-z0-9][A-Za-z0-9._-]*)?(?:\+[A-Za-z0-9][A-Za-z0-9._-]*)?"""
    ).matchEntire(versionName) ?: throw GradleException(
        "Invalid appVersionName '$versionName': expected Major.Minor[.Patch] with optional -channel/+build suffix, e.g. 2.2.0-IntDev_rev0."
    )
    val seed = "GlyphBarty:${version.groupValues[1]}.${version.groupValues[2]}"
    val firstByte = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))[0].toInt() and 0xff
    return seed to (firstByte % 5) + 1
}

val (personalitySeed, personalityNumber) = rollVersionPersonality(appVersionName)
val buildContentHash = MessageDigest.getInstance("SHA-256").run {
    buildNumberFiles.forEach { file ->
        update(file.relativeTo(rootProject.projectDir).invariantSeparatorsPath.toByteArray())
        update(0)
        update(file.readBytes())
        update(0)
    }
    digest().joinToString("") { "%02x".format(it) }
}
val buildTimestamp = DateTimeFormatter.ofPattern("yyMMdd-HHmm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(buildNumberFiles.maxOf { it.lastModified() }))
val buildNumberVersionPrefix = if (
    appVersionName.substringAfter('-', missingDelimiterValue = "")
        .startsWith("IntDev", ignoreCase = true)
) {
    "d"
} else {
    "v"
}
val generatedBuildNumber =
    "$appCodeName-$buildNumberVersionPrefix${appVersionName.substringBefore('-')}-$buildTimestamp-${buildContentHash.take(3)}"

android {
    namespace = "jp.linkserver.glyphvisualizer"

    compileSdk = 37

    defaultConfig {
        applicationId = "jp.linkserver.glyphvisualizer"
        minSdk = 33
        targetSdk = 36
        versionCode = 15
        versionName = appVersionName
        buildConfigField("String", "BUILD_NUMBER", "\"$generatedBuildNumber\"")
        buildConfigField("String", "CODE_NAME", "\"$appCodeName\"")
        buildConfigField("String", "PERSONALITY_SEED", "\"$personalitySeed\"")
        buildConfigField("int", "PERSONALITY_NUMBER", personalityNumber.toString())

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

// Exercise the actual build-time implementation, rather than a runtime copy of it.
val verifyVersionPersonality by tasks.registering {
    group = "verification"
    description = "Verifies deterministic version personality rolls and invalid-version diagnostics."
    doLast {
        val reference = "GlyphBarty:2.2" to 4
        listOf("2.2", "2.2.0", "2.2.9", "2.2.0-IntDev_rev0", "2.2.1-Beta1", "2.2.9+build.123").forEach {
            check(rollVersionPersonality(it) == reference) { "Unexpected personality for $it" }
        }
        check(rollVersionPersonality("2.10.0") == ("GlyphBarty:2.10" to 5))
        check(rollVersionPersonality("2.10").first != rollVersionPersonality("2.1").first)
        // 229 exercises unsigned byte conversion (a signed Byte would be -27).
        check(rollVersionPersonality("4.3.0") == ("GlyphBarty:4.3" to 5))
        listOf("", "2", "v2.2.0", "2.x.0", "2.2.0.1", "2.2.0-", "2.2.0+", "-1.2.0", "2.02.0", " 2.2.0", "2.2.0\n").forEach {
            val error = runCatching { rollVersionPersonality(it) }.exceptionOrNull()
            check(error is GradleException && error.message.orEmpty().contains("Invalid appVersionName")) {
                "Missing actionable error for invalid version '$it'"
            }
        }
        logger.lifecycle("Version personality verification passed: patch/channel invariance, string minor, unsigned SHA-256 byte, invalid versions.")
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    dependsOn(verifyVersionPersonality)
}

dependencies {
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
