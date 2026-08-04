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

dependencies {
    api(project(":abi-mvp"))
    api(project(":abi-cm"))
    api(project(":host-api"))
    api(libs.wasmtime4j)
    // JNI implementation required on both desktop and Android (ART).
    api(libs.wasmtime4j.jni)
    // Desktop/CI natives (glibc/Darwin/Windows). Android consumers must exclude this
    // and ship Bionic libwasmtime4j.so via android-natives/jniLibs instead.
    runtimeOnly(libs.wasmtime4j.native)
    testImplementation(libs.junit)
}

tasks.test {
    systemProperty("wasmtime4j.runtime", "jni")
    val guestWasm = rootProject.file("guest/vector-add/vector_add.wasm")
    inputs.file(guestWasm)
    systemProperty("wasi.webgpu.guest.vectorAdd", guestWasm.absolutePath)

    val guestCm = rootProject.file("guest/vector-add-cm/vector_add_cm.wasm")
    inputs.file(guestCm)
    systemProperty("wasi.webgpu.guest.vectorAddCm", guestCm.absolutePath)
}