package com.likethesalad.tools.artifact.publisher.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

abstract class VersionBumpTask : DefaultTask() {
    companion object {
        private val VERSION_PATTERN: Regex = Regex("^(\\d+)\\.(\\d+)[\\d.]+")
    }

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun execute() {
        val gradlePropertiesFile = File("gradle.properties")
        val gradleProperties = Properties()

        gradlePropertiesFile.reader().use {
            gradleProperties.load(it)
            val newVersion = bumpVersion(version.get())
            gradleProperties.setProperty("version", newVersion)
        }
        gradlePropertiesFile.writer().use {
            gradleProperties.store(it, null)
        }
    }

    private fun bumpVersion(version: String): String {
        log("Bumping minor version for: $version")
        val versionMatcher = VERSION_PATTERN.matchEntire(version)
        if (versionMatcher != null) {
            val currentMinorVersion = versionMatcher.groupValues[2].toInt()
            log("Current minor version is: $currentMinorVersion")
            val newVersion = versionMatcher.groupValues[1] + "." + (currentMinorVersion + 1) + ".0"
            log("The new version is: $newVersion")
            return newVersion
        } else {
            throw IllegalArgumentException("Could not find minor version in: $version")
        }
    }

    private fun log(message: String) {
        logger.lifecycle(message)
    }
}