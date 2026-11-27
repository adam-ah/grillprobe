import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "app.grillprobe"
    compileSdk = 36

    val buildNumber =
        (providers.environmentVariable("BUILD_NUMBER").orNull ?: providers.environmentVariable("GITHUB_RUN_NUMBER").orNull)
            ?.toIntOrNull() ?: 1
    val keystorePath = providers.environmentVariable("SIGNING_KEYSTORE_PATH").orNull

    defaultConfig {
        applicationId = "app.grillprobe"
        minSdk = 31
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (keystorePath != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkAllWarnings = true
        checkTestSources = true
        checkDependencies = true
        disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("screenshots.dir", rootProject.file("screenshots").absolutePath)
                it.maxHeapSize = "2g"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = true
    parallel = true
    autoCorrect = providers.gradleProperty("fix").isPresent
    config.setFrom(rootProject.file("config/detekt.yml"))
    source.setFrom(
        "src/main/java",
        "src/test/java",
        rootProject.file("lint-rules/src/main/kotlin"),
        rootProject.file("lint-rules/src/test/kotlin"),
    )
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    detektPlugins(project(":lint-rules"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
}
