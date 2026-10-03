import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    java
    idea
    eclipse
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

version = property("mod_version") as String
group = property("mod_group_id") as String

base {
    archivesName.set(property("mod_id") as String)
}

val pycodersRuntimeProjectId = providers.gradleProperty("pycodersRuntimeProjectId").orElse(rootProject.name).get()
val pycodersConfiguredRunDir = providers.gradleProperty("pycodersRuntimeDir").orNull?.let { file(it).canonicalFile }
val pycodersConfiguredRuntimeRoot = providers.gradleProperty("pycodersRuntimeRoot").orNull
    ?: providers.environmentVariable("MMTL_WORKSPACE_RUNTIME_ROOT").orNull
val pycodersConfiguredRootRunDir = pycodersConfiguredRuntimeRoot?.let { File(it, "legacy-import/$pycodersRuntimeProjectId/run").canonicalFile }
val pycodersDiscoveredRunDir = generateSequence(project.projectDir.canonicalFile) { it.parentFile }
    .map { File(it, "runtime/legacy-import/$pycodersRuntimeProjectId/run").canonicalFile }
    .firstOrNull { it.isDirectory }
val pycodersRunDir = pycodersConfiguredRunDir ?: pycodersConfiguredRootRunDir ?: pycodersDiscoveredRunDir ?: file("run").canonicalFile
fun decodeArgs(name: String): List<String> = providers.gradleProperty(name).orNull?.takeIf { it.isNotEmpty() }?.split('.')?.map { if (it == "_") "" else String(Base64.getDecoder().decode(it), StandardCharsets.UTF_8) } ?: emptyList()
val pycodersGameArgs = decodeArgs("pycodersGameArgsB64")
val pycodersJavaArgs = decodeArgs("pycodersJavaArgsB64")
val pycodersUsername = providers.gradleProperty("pycodersUsername").orElse("Dev").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

minecraft {
    mappings("official", property("minecraft_version") as String)
    copyIdeResources.set(true)

    runs {
        create("client") {
            workingDirectory(pycodersRunDir)
            args("--username", pycodersUsername)
            pycodersGameArgs.forEach { args(it) }
            pycodersJavaArgs.forEach { jvmArg(it) }
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(pycodersRunDir)
            pycodersGameArgs.forEach { args(it) }
            pycodersJavaArgs.forEach { jvmArg(it) }
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args("--nogui")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("data") {
            workingDirectory(pycodersRunDir)
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args(
                "--mod", property("mod_id") as String,
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources/")
            )
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

repositories {
    maven("https://maven.minecraftforge.net/")
    mavenCentral()
}

dependencies {
    minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.processResources {
    val replaceProperties = mapOf(
        "loader_version_range" to project.property("loader_version_range"),
        "mod_license" to project.property("mod_license"),
        "mod_id" to project.property("mod_id"),
        "mod_version" to project.property("mod_version"),
        "mod_name" to project.property("mod_name"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description"),
        "forge_version_range" to project.property("forge_version_range"),
        "minecraft_version_range" to project.property("minecraft_version_range")
    )
    inputs.properties(replaceProperties)
    filesMatching("META-INF/mods.toml") {
        expand(replaceProperties)
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to project.property("mod_id"),
                "Specification-Vendor" to project.property("mod_authors"),
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to project.property("mod_authors")
            )
        )
    }
    finalizedBy("reobfJar")
}

