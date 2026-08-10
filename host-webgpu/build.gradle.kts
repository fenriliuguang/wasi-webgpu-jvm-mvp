plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.fenriliuguang.wasi.webgpu.experimental"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Local publishToMavenLocal self-check only (see docs/maven-local.md).
    publishing {
        singleVariant("release")
    }
}

dependencies {
    api(project(":host-api"))
    api(libs.androidx.webgpu)
    implementation(libs.androidx.core.ktx)
    androidTestImplementation(libs.androidx.junit)
}

apply(from = rootProject.file("gradle/wasi-webgpu-publishing.gradle.kts"))

