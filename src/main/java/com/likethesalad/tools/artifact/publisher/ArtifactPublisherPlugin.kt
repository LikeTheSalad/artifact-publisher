package com.likethesalad.tools.artifact.publisher

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherTargetExtension
import com.likethesalad.tools.artifact.publisher.publications.AarMavenPublicationCreator
import com.likethesalad.tools.artifact.publisher.publications.JarMavenPublicationCreator
import com.likethesalad.tools.artifact.publisher.publications.MavenPublicationCreator
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin

class ArtifactPublisherPlugin : Plugin<Project> {

    companion object {
        private const val EXTENSION_ARTIFACT_PUBLISHER_NAME = "artifactPublisher"
        private const val EXTENSION_ARTIFACT_PUBLISHER_TARGET_NAME = "artifactPublisherTarget"
    }

    private lateinit var extension: ArtifactPublisherExtension

    override fun apply(project: Project) {
        verifyRootProject(project)
        applyRootProjectPlugins(project.plugins)
        extension = project.extensions.create(EXTENSION_ARTIFACT_PUBLISHER_NAME, ArtifactPublisherExtension::class.java)
        configureExtensionDefaults(project)

        project.subprojects { subProject ->
            configureSubproject(subProject)
        }

        configurePublishing(project)
    }

    private fun configureExtensionDefaults(project: Project) {
        extension.description.set(project.provider { project.description })
        extension.version.set(project.provider { project.version.toString() })
        extension.group.set(project.provider { project.group.toString() })
    }

    private fun configureSubproject(subProject: Project) {
        val plugins = subProject.plugins

        // For Java libraries
        plugins.withId("java-library") {
            configurePublishTarget(subProject, JarMavenPublicationCreator(extension))
        }

        // For Android libraries
        plugins.withId("com.android.library") {
            configurePublishTarget(subProject, AarMavenPublicationCreator(extension))
        }
    }

    private fun configurePublishTarget(
        subProject: Project,
        mavenPublicationCreator: MavenPublicationCreator
    ) {
        applySubprojectPlugins(subProject.plugins)
        val publishing = subProject.extensions.getByType(PublishingExtension::class.java)
        val targetExtension = subProject.extensions.create(
            EXTENSION_ARTIFACT_PUBLISHER_TARGET_NAME,
            ArtifactPublisherTargetExtension::class.java
        )
        subProject.afterEvaluate {
            if (!targetExtension.disablePublishing.get()) {
                setPropertiesFromRoot(subProject)
                val mainPublication = mavenPublicationCreator.create(subProject, publishing)
                signPublication(subProject, mainPublication)
            }
        }
    }

    private fun setPropertiesFromRoot(subProject: Project) {
        subProject.version = subProject.rootProject.version
        subProject.group = subProject.rootProject.group
    }

    private fun verifyRootProject(project: Project) {
        if (project != project.rootProject) {
            throw IllegalStateException("This plugin can only be applied to the root project")
        }
    }

    private fun signPublication(project: Project, publication: MavenPublication) {
        val signing = project.extensions.getByType(SigningExtension::class.java)
        signing.sign(publication)
    }

    private fun applySubprojectPlugins(plugins: PluginContainer) {
        plugins.apply(MavenPublishPlugin::class.java)
        plugins.apply(SigningPlugin::class.java)
        plugins.apply(DokkaPlugin::class.java)
    }

    private fun applyRootProjectPlugins(plugins: PluginContainer) {
        plugins.apply(NexusPublishPlugin::class.java)
    }

    private fun configurePublishing(project: Project) {
        val nexusPublishingExtension = project.extensions.getByType(NexusPublishExtension::class.java)
        nexusPublishingExtension.repositories {
            it.sonatype()
        }
        createPublishingTask(project)
    }

    private fun createPublishingTask(project: Project) {
        val tasks = project.tasks
        val finishReleaseTask = tasks.named("closeAndReleaseSonatypeStagingRepository")
        val closeTask = tasks.named("closeSonatypeStagingRepository")

        project.subprojects { subProject ->
            subProject.tasks.whenTaskAdded { subProjectTask ->
                if (subProjectTask.name == "publishMainPublicationToSonatypeRepository") {
                    closeTask.configure {
                        it.dependsOn(subProjectTask)
                    }
                }
            }
        }

        tasks.register("publishToMavenCentral") {
            it.dependsOn(finishReleaseTask)
        }
    }
}