/**
 * Local publishability engineering for core library modules.
 *
 * - Pins group / version from root gradle.properties
 * - Enables `publishToMavenLocal` self-check only
 * - Does NOT configure remote repositories or claim an external release
 *
 * For `com.android.library`, the module must declare
 * `android.publishing.singleVariant("release")` before applying this script.
 */

apply(plugin = "maven-publish")

val wasiGroup =
    (findProperty("wasi.webgpu.group") as String?)
        ?: error("Missing gradle.properties key: wasi.webgpu.group")
val wasiVersion =
    (findProperty("wasi.webgpu.version") as String?)
        ?: error("Missing gradle.properties key: wasi.webgpu.version")

group = wasiGroup
version = wasiVersion

fun MavenPublication.applyWasiCoordinates() {
    groupId = wasiGroup
    artifactId = project.name
    version = wasiVersion
    pom {
        name.set("wasi-webgpu-jvm-mvp:${project.name}")
        description.set(
            "experimental Dawn/CPU host mapping for wasi:webgpu — NOT a compliant " +
                "wasi:webgpu product. Local publishability engineering only " +
                "(publishToMavenLocal self-check); no external release and not a " +
                "public dependency.",
        )
    }
}

val publishingExt = extensions.getByType(PublishingExtension::class.java)

when {
    pluginManager.hasPlugin("com.android.library") -> {
        publishingExt.publications.create<MavenPublication>("maven") {
            applyWasiCoordinates()
        }
        afterEvaluate {
            val publication =
                publishingExt.publications.getByName("maven") as MavenPublication
            publication.from(components.getByName("release"))
        }
    }
    pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") ||
        pluginManager.hasPlugin("java") ||
        pluginManager.hasPlugin("java-library") -> {
        publishingExt.publications.create<MavenPublication>("maven") {
            from(components.getByName("java"))
            applyWasiCoordinates()
        }
    }
    else ->
        error(
            "${project.path}: apply gradle/wasi-webgpu-publishing.gradle.kts only to " +
                "kotlin.jvm or com.android.library modules",
        )
}
