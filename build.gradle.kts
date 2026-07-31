import java.util.regex.Matcher
import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

val syncReadmeVersion by tasks.registering {
    group = "documentation"
    description = "Syncs Jupiter SDK / VM SDK versions from app/build.gradle.kts into README.md"

    doLast {
        val readmeFile = layout.projectDirectory.file("README.md").asFile
        val appGradleFile = layout.projectDirectory.file("app/build.gradle.kts").asFile
        val gradlePropertiesFile = layout.projectDirectory.file("gradle.properties").asFile
        if (!readmeFile.exists()) return@doLast
        if (!appGradleFile.exists()) return@doLast
        if (!gradlePropertiesFile.exists()) return@doLast

        fun replaceBetweenMarkers(content: String, start: String, end: String, body: String): String {
            val replacement = buildString {
                appendLine(start)
                appendLine(body)
                append(end)
            }
            val pattern = Regex("$start[\\s\\S]*?$end")
            return if (pattern.containsMatchIn(content)) {
                content.replace(pattern, Matcher.quoteReplacement(replacement))
            } else {
                "$content\n\n$replacement\n"
            }
        }

        val appGradleText = appGradleFile.readText()
        val gradleProperties = Properties().apply {
            gradlePropertiesFile.inputStream().use { load(it) }
        }
        val jupiterVmSdkVersion = gradleProperties.getProperty("JUPITER_VM_SDK_VERSION", "unknown").trim()
        val implementationLines = appGradleText
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("implementation(") }
            .toList()
        val dependenciesBlockBody = buildString {
            appendLine("dependencies {")
            implementationLines.forEach { line ->
                appendLine("    $line")
            }
            append("}")
        }

        val original = readmeFile.readText()
        val withVmSdk = replaceBetweenMarkers(
            content = original,
            start = "<!-- JUPITER_VM_SDK_VERSION_START -->",
            end = "<!-- JUPITER_VM_SDK_VERSION_END -->",
            body = "Jupiter VM SDK version: $jupiterVmSdkVersion"
        )
        val updated = replaceBetweenMarkers(
            content = withVmSdk,
            start = "<!-- APP_DEPENDENCIES_START -->",
            end = "<!-- APP_DEPENDENCIES_END -->",
            body = dependenciesBlockBody
        )

        if (original != updated) {
            readmeFile.writeText(updated)
            println("README.md synced from app/build.gradle.kts dependencies, vm=$jupiterVmSdkVersion")
        }
    }
}

subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("androidx.core:core:1.13.1")
            force("androidx.core:core-ktx:1.13.1")
            force("androidx.activity:activity:1.9.0")
        }
    }
}

tasks.matching {
    it.name == "prepareKotlinBuildScriptModel" ||
        it.name == "prepareKotlinBuildScriptModelForAndroid" ||
        it.name == "preBuild"
}.configureEach {
    dependsOn(syncReadmeVersion)
}

subprojects {
    tasks.matching {
        it.name == "prepareKotlinBuildScriptModel" ||
            it.name == "prepareKotlinBuildScriptModelForAndroid" ||
            it.name == "preBuild"
    }.configureEach {
        dependsOn(rootProject.tasks.named("syncReadmeVersion"))
    }
}
