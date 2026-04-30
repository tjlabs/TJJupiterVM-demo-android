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
        if (!readmeFile.exists()) return@doLast
        if (!appGradleFile.exists()) return@doLast

        fun replaceBetweenMarkers(content: String, start: String, end: String, body: String): String {
            val replacement = buildString {
                appendLine(start)
                appendLine(body)
                append(end)
            }
            val pattern = Regex("$start[\\s\\S]*?$end")
            return if (pattern.containsMatchIn(content)) {
                content.replace(pattern, replacement)
            } else {
                "$content\n\n$replacement\n"
            }
        }

        val appGradleText = appGradleFile.readText()
        val jupiterSdkVersion = Regex("""val\s+jupiterSdkVersion\s*=\s*"([^"]+)"""")
            .find(appGradleText)
            ?.groupValues
            ?.getOrNull(1)
            ?: "unknown"
        val jupiterVmAarName = Regex("""val\s+jupiterVmAarName\s*=\s*"([^"]+)"""")
            .find(appGradleText)
            ?.groupValues
            ?.getOrNull(1)
            ?: "unknown"

        val original = readmeFile.readText()
        val withJupiterSdk = replaceBetweenMarkers(
            content = original,
            start = "<!-- JUPITER_SDK_VERSION_START -->",
            end = "<!-- JUPITER_SDK_VERSION_END -->",
            body = "Jupiter SDK version: $jupiterSdkVersion"
        )
        val updated = replaceBetweenMarkers(
            content = withJupiterSdk,
            start = "<!-- JUPITER_VM_SDK_VERSION_START -->",
            end = "<!-- JUPITER_VM_SDK_VERSION_END -->",
            body = "Jupiter VM SDK (AAR): $jupiterVmAarName"
        )

        if (original != updated) {
            readmeFile.writeText(updated)
            println("README.md versions synced: jupiter=$jupiterSdkVersion, vm=$jupiterVmAarName")
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
