package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

class JarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun getSourcesJarTask(project: Project): TaskProvider<Jar> {
        return getJarSourcesJarTask(project)
    }

    private fun getJarSourcesJarTask(project: Project): TaskProvider<Jar> {
        return project.tasks.register("sourcesJar", Jar::class.java) {
            it.from(getSourceSets(project).getByName("main").allSource)
            it.archiveClassifier.set("sources")
        }
    }

    private fun getSourceSets(project: Project): SourceSetContainer {
        return project.extensions.getByType(SourceSetContainer::class.java)
    }

    override fun getComponentName(): String {
        return "java"
    }
}