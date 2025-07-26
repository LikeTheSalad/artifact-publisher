package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

abstract class MavenPublicationCreator(private val extension: ArtifactPublisherExtension) {

    companion object {
        private const val PUBLICATION_NAME = "likethesalad"
    }

    fun create(project: Project, publishing: PublishingExtension): MavenPublication {
        return publishing.publications.create(PUBLICATION_NAME, MavenPublication::class.java) { publication ->
            publication.from(project.components.getByName(getComponentName()))
            configureCommonPublicationParams(publication)
        }
    }

    abstract fun prepare(project: Project, isRelease: Boolean)

    abstract fun getComponentName(): String

    protected fun enableMavenCentralPublishing(subProject: Project) {
        val extension = subProject.extensions.getByType(MavenPublishBaseExtension::class.java)
        extension.publishToMavenCentral(true)
    }

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