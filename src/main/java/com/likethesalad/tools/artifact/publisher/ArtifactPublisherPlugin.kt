package com.likethesalad.tools.artifact.publisher

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin

class ArtifactPublisherPlugin : Plugin<Project> {

    private lateinit var extension: ArtifactPublisherExtension

    override fun apply(project: Project) {
        verifyRootProject(project)
        extension = project.extensions.create("artifactPublisher", ArtifactPublisherExtension::class.java)
        applyPlugins(project.plugins)
        val mainPublication = createMainMavenPublication(project)
        signPublication(project, mainPublication)
    }

    fun verifyRootProject(project: Project) {
        if (project != project.rootProject) {
            throw IllegalStateException("This plugin can only be applied to the root project")
        }
    }

    private fun createMainMavenPublication(project: Project): MavenPublication {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)

        return publishing.publications.create("main", MavenPublication::class.java) { publication ->
            publication.groupId = extension.group.get()
            publication.version = extension.version.get()

            publication.artifact(getSourcesJarTask(project))
            publication.artifact(getJavadocJarTask(project))
            publication.from(project.components.getByName("java"))

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
    }

    private fun signPublication(project: Project, publication: MavenPublication) {
        val signing = project.extensions.getByType(SigningExtension::class.java)
        signing.sign(publication)
    }

    private fun getJavadocJarTask(project: Project): TaskProvider<Jar> {
        return project.tasks.register("javadocJar", Jar::class.java) {
            it.from(project.tasks.named("dokkaHtml"))
            it.archiveClassifier.set("javadoc")
        }
    }

    private fun getSourcesJarTask(project: Project): TaskProvider<Jar> {
        return project.tasks.register("sourcesJar", Jar::class.java) {
            it.from(getSourceSets(project).getByName("main").allSource)
            it.archiveClassifier.set("sources")
        }
    }

    private fun getSourceSets(project: Project): SourceSetContainer {
        return project.extensions.getByType(SourceSetContainer::class.java)
    }

    private fun applyPlugins(plugins: PluginContainer) {
        plugins.apply(MavenPublishPlugin::class.java)
        plugins.apply(SigningPlugin::class.java)
        plugins.apply(DokkaPlugin::class.java)
    }
}