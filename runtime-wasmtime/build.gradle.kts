plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
}

/** Host platform triple + lib name matching Maven wasmtime4j-native jar layout. */
fun desktopNativePlatform(): Pair<String, String>? {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isX64 = arch == "amd64" || arch == "x86_64"
    val isArm64 = arch == "aarch64" || arch == "arm64"
    return when {
        os.contains("win") && isX64 -> "windows-x86_64" to "wasmtime4j.dll"
        os.contains("linux") && isX64 -> "linux-x86_64" to "libwasmtime4j.so"
        os.contains("linux") && isArm64 -> "linux-aarch64" to "libwasmtime4j.so"
        (os.contains("mac") || os.contains("darwin")) && isArm64 ->
            "darwin-aarch64" to "libwasmtime4j.dylib"
        else -> null
    }
}

val desktopPlatform = desktopNativePlatform()
val desktopNativeFile = desktopPlatform?.let { (dir, name) ->
    layout.projectDirectory.file("desktop-natives/$dir/$name").asFile
}
val hasDesktopCmNatives = desktopNativeFile?.isFile == true

val upstreamWasmtime4jNative by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    api(project(":abi-mvp"))
    api(project(":abi-cm"))
    api(project(":host-api"))
    api(libs.wasmtime4j)
    // JNI implementation required on both desktop and Android (ART).
    api(libs.wasmtime4j.jni)
    upstreamWasmtime4jNative(libs.wasmtime4j.native)
    testImplementation(libs.junit)
}

val patchedWasmtime4jNativeJar =
    if (hasDesktopCmNatives) {
        val platform = desktopPlatform!!
        val nativeFile = desktopNativeFile!!
        val entryPath = "natives/${platform.first}/${platform.second}"
        tasks.register<Zip>("patchedWasmtime4jNativeJar") {
            from({
                zipTree(upstreamWasmtime4jNative.singleFile).matching {
                    exclude(entryPath)
                }
            })
            from(nativeFile) {
                into("natives/${platform.first}")
                rename { platform.second }
            }
            archiveFileName.set("wasmtime4j-native-cm-patched.jar")
            destinationDirectory.set(layout.buildDirectory.dir("libs"))
            inputs.file(nativeFile)
            inputs.files(upstreamWasmtime4jNative)
        }
    } else {
        null
    }

dependencies {
    if (patchedWasmtime4jNativeJar != null) {
        // Prefer CM-patched jar; do not also pull upstream natives (would race on extract).
        runtimeOnly(files(patchedWasmtime4jNativeJar.map { it.archiveFile }))
        testRuntimeOnly(files(patchedWasmtime4jNativeJar.map { it.archiveFile }))
    } else {
        // Desktop/CI natives (glibc/Darwin/Windows). Android consumers must exclude this
        // and ship Bionic libwasmtime4j.so via android-natives/jniLibs instead.
        runtimeOnly(libs.wasmtime4j.native)
    }
}

tasks.test {
    if (patchedWasmtime4jNativeJar != null) {
        dependsOn(patchedWasmtime4jNativeJar)
    }
    systemProperty("wasmtime4j.runtime", "jni")
    val guestWasm = rootProject.file("guest/vector-add/vector_add.wasm")
    inputs.file(guestWasm)
    systemProperty("wasi.webgpu.guest.vectorAdd", guestWasm.absolutePath)

    val guestCm = rootProject.file("guest/vector-add-cm/vector_add_cm.wasm")
    inputs.file(guestCm)
    systemProperty("wasi.webgpu.guest.vectorAddCm", guestCm.absolutePath)

    // CM tests need patched natives under desktop-natives/ (or explicit force).
    systemProperty("wasi.webgpu.cm.natives", hasDesktopCmNatives.toString())
    if (desktopNativeFile != null) {
        systemProperty("wasi.webgpu.cm.natives.path", desktopNativeFile.absolutePath)
    }
}
