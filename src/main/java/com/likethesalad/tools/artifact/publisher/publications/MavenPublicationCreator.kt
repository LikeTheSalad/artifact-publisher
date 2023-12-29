package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

abstract class MavenPublicationCreator(private val extension: ArtifactPublisherExtension) {

    fun create(project: Project, publishing: PublishingExtension, isRelease: Boolean): MavenPublication {
        onPrepare(project, isRelease)

        return publishing.publications.create("likethesalad", MavenPublication::class.java) { publication ->
            publication.from(project.components.getByName(getComponentName()))
            configureCommonPublicationParams(publication)
        }
    }

    abstract fun onPrepare(project: Project, isRelease: Boolean)

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