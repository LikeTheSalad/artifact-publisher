package com.likethesalad.tools.artifact.publisher.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

abstract class ChangelogUpdaterTask : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun execute() {
        val unreleasedRegex = Regex("## (Unreleased)")
        val changelogFile = File("CHANGELOG.md")
        val changelog = changelogFile.readText()
        val unreleasedMatch =
            unreleasedRegex.find(changelog) ?: throw IllegalStateException(""""## Unreleased" not found""")

        val replaceablePart = unreleasedMatch.groups[1]!!

        val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val updatedChangelog = changelog.replaceRange(replaceablePart.range, "Version ${version.get()} ($currentDate)")

        changelogFile.writeText(updatedChangelog)
    }
}