package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import com.likethesalad.tools.artifact.publisher.tools.AndroidSourceSetsHelper
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import java.io.File

class AarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun getSourcesJarTask(project: Project): TaskProvider<Jar> {
        return getAndroidSourcesJarTask(project)
    }

    private fun getAndroidSourcesJarTask(project: Project): TaskProvider<Jar> {
        return project.tasks.register("androidSourcesJar", Jar::class.java) {
            it.from(getAndroidSourceSets(project))
            it.archiveClassifier.set("sources")
        }
    }

    private fun getAndroidSourceSets(project: Project): Set<File> {
        val kotlinExtension = project.extensions.getByName("kotlin")
        return AndroidSourceSetsHelper.getAndroidSourceSets(kotlinExtension)
    }

    override fun getComponentName(): String {
        return "release"
    }
}