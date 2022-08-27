package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

abstract class MavenPublicationCreator(private val extension: ArtifactPublisherExtension) {

    fun create(project: Project, publishing: PublishingExtension, addExtraArtifacts: Boolean): MavenPublication {
        return publishing.publications.create("likethesalad", MavenPublication::class.java) { publication ->
            publication.from(project.components.getByName(getComponentName()))

            if (addExtraArtifacts) {
                publication.artifact(getSourcesJarTask(project))
                publication.artifact(getJavadocJarTask(project))
            }

            configureCommonPublicationParams(publication)
        }
    }

    abstract fun getSourcesJarTask(project: Project): TaskProvider<Jar>

    abstract fun getComponentName(): String

    private fun configureCommonPublicationParams(publication: MavenPublication) {
        publication.groupId = getGroup()
        publication.version = getVersion()

        publication.pom {
            it.name.set(extension.displayName)
            it.description.set(extension.description)
            it.url.set(extension.url)
            it.licenses { licenses ->
                licenses.license { license ->
                    license.name.set("MIT License")
                    license.url.set("https://opensource.org/licenses/MIT")
                }
            }
            it.developers { developers ->
                developers.developer { developer ->
                    developer.id.set("LikeTheSalad")
                    developer.name.set("Cesar Munoz")
                    developer.email.set("likethesalad@gmail.com")
                }
            }
            it.scm { scm ->
                scm.url.set(extension.url)
                scm.connection.set(extension.vcsUrl)
            }
            it.issueManagement { issueManagement ->
                issueManagement.url.set(extension.issueTrackerUrl)
            }
        }
    }

    private fun getJavadocJarTask(project: Project): TaskProvider<Jar> {
        return project.tasks.register("javadocJar", Jar::class.java) {
            it.from(project.tasks.named("dokkaHtml"))
            it.archiveClassifier.set("javadoc")
        }
    }

    private fun getVersion(): String {
        val version = extension.version.get()
        if (version.isEmpty() || version == "unspecified") {
            throw IllegalArgumentException("Version not set")
        }

        return version
    }

    private fun getGroup(): String {
        val group = extension.group.get()
        if (group.isEmpty()) {
            throw IllegalArgumentException("Group not set")
        }

        return group
    }
}