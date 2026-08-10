plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

/** Local self-check only — not an external release. See docs/maven-local.md. */
tasks.register("publishEngineeredToMavenLocal") {
    group = "publishing"
    description =
        "publishToMavenLocal for engineered modules " +
            "(host-api, host-webgpu, abi-mvp, abi-cm, abi-wasi); no remote upload"
    dependsOn(
        ":host-api:publishToMavenLocal",
        ":host-webgpu:publishToMavenLocal",
        ":abi-mvp:publishToMavenLocal",
        ":abi-cm:publishToMavenLocal",
        ":abi-wasi:publishToMavenLocal",
    )
}

