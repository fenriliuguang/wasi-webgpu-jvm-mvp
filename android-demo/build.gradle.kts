plugins {
    alias(libs.plugins.android.application)
}

val guestWasm = rootProject.file("guest/vector-add/vector_add.wasm")
val guestCmWasm = rootProject.file("guest/vector-add-cm/vector_add_cm.wasm")
val guestTriangleCmWasm = rootProject.file("guest/triangle-cm/triangle_cm.wasm")
val guestCubeCmWasm = rootProject.file("guest/cube-cm/cube_cm.wasm")
// Plain File (not Provider): AGP 9 rejects Provider in SourceSet.assets.srcDir.
val generatedAssetsDir = layout.buildDirectory.get().asFile.resolve("generated/assets")

val syncGuestAssets by tasks.registering(Copy::class) {
    from(guestWasm) {
        rename { "vector_add.wasm" }
    }
    from(guestCmWasm) {
        rename { "vector_add_cm.wasm" }
    }
    from(guestTriangleCmWasm) {
        rename { "triangle_cm.wasm" }
    }
    from(guestCubeCmWasm) {
        rename { "cube_cm.wasm" }
    }
    into(generatedAssetsDir.resolve("guest"))
}

// Strip upstream classes so our Android-tolerant copies win (ARM64 jlong / u64 handles).
val filteredWasmtime4jDir = layout.buildDirectory.dir("filtered-deps")
val filteredWasmtime4jJar = filteredWasmtime4jDir.map { it.file("wasmtime4j-no-validation.jar") }
val wasmtime4jForFilter = configurations.detachedConfiguration(
    dependencies.create("ai.tegmentum:wasmtime4j:${libs.versions.wasmtime4j.get()}") {
        isTransitive = false
    },
)
val filterWasmtime4jJar by tasks.registering(Jar::class) {
    dependsOn(wasmtime4jForFilter)
    from({
        zipTree(wasmtime4jForFilter.singleFile).matching {
            exclude("ai/tegmentum/wasmtime4j/util/Validation.class")
            // Host-callback JSON: unsigned u64 > Long.MAX_VALUE (native-window handles).
            // Exclude outer + nested ($JsonParser, $1, …) so the local Java copy wins.
            exclude("ai/tegmentum/wasmtime4j/component/ConcurrentCallCodec*.class")
        }
    })
    archiveFileName.set("wasmtime4j-no-validation.jar")
    destinationDirectory.set(filteredWasmtime4jDir)
}

android {
    namespace = "io.github.fenriliuguang.wasi.webgpu.demo"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.fenriliuguang.wasi.webgpu.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-experimental"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Match prebuilt libwasmtime4j.so ABIs under runtime-wasmtime/android-natives.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(rootProject.file("runtime-wasmtime/android-natives/jniLibs"))
            assets.srcDir(generatedAssetsDir)
        }
    }

    packaging {
        jniLibs {
            // Ensure libwasmtime4j.so is extractable for System.loadLibrary on all API levels.
            useLegacyPackaging = true
        }
        resources {
            // Strip desktop wasmtime4j-native JAR payloads if they ever leak onto the classpath.
            excludes += listOf(
                "natives/**",
                "META-INF/maven/ai.tegmentum/wasmtime4j-native/**",
            )
        }
    }
}

configurations.configureEach {
    // Desktop glibc/darwin/windows natives must not be used on Android (Bionic).
    exclude(group = "ai.tegmentum", module = "wasmtime4j-native")
    // Replace with filtered jar + local Validation / ConcurrentCallCodec (ARM64 opaque handles).
    exclude(group = "ai.tegmentum", module = "wasmtime4j")
}

tasks.named("preBuild").configure {
    dependsOn(syncGuestAssets)
    dependsOn(filterWasmtime4jJar)
}
tasks.configureEach {
    if (name.startsWith("merge") && name.contains("Assets")) {
        dependsOn(syncGuestAssets)
    }
    if (name.contains("compile", ignoreCase = true) && name.contains("Kotlin", ignoreCase = true)) {
        dependsOn(filterWasmtime4jJar)
    }
}

dependencies {
    implementation(project(":host-webgpu"))
    implementation(project(":runtime-wasmtime"))
    implementation(files(filteredWasmtime4jJar).builtBy(filterWasmtime4jJar))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // Lets AGP/UTP install androidx.test.services (needed on API 30+ / OEM devices).
    androidTestUtil(libs.androidx.test.services)
}
