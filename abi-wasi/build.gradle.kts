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
    // Constants-only module for wasi:webgpu@0.3.0-rc.2 CM import paths (slice B).
    // Host bindings arrive in C+; keep free of host-api for a thin ABI identity layer.
    testImplementation(libs.junit)
}
