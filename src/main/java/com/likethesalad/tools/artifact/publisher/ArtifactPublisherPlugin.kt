package com.likethesalad.tools.artifact.publisher

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
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
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class ArtifactPublisherPlugin : Plugin<Project> {

    private lateinit var extension: ArtifactPublisherExtension

    override fun apply(project: Project) {
        verifyRootProject(project)
        applyRootProjectPlugins(project.plugins)
        extension = project.extensions.create("artifactPublisher", ArtifactPublisherExtension::class.java)

        project.subprojects { subProject ->
            configureSubproject(subProject)
        }

        createPublishToMavenCentralTask(project)
    }

    private fun configureSubproject(project: Project) {
        applySubprojectPlugins(project.plugins)
        val plugins = project.plugins
        val publishing = project.extensions.getByType(PublishingExtension::class.java)

        // For Java libraries
        plugins.withId("java-library") {
            val mainPublication = createJarMavenPublication(project, publishing)
            signPublication(project, mainPublication)
        }

        // For Android libraries
        plugins.withId("com.android.library") {
            project.afterEvaluate {
                val mainPublication = createAarMavenPublication(project, publishing)
                signPublication(project, mainPublication)
            }
        }
    }

    private fun verifyRootProject(project: Project) {
        if (project != project.rootProject) {
            throw IllegalStateException("This plugin can only be applied to the root project")
        }
    }

    private fun createAarMavenPublication(project: Project, publishing: PublishingExtension): MavenPublication {
        return publishing.publications.create("main", MavenPublication::class.java) { publication ->
            publication.artifact(getAndroidSourcesJarTask(project))
            publication.from(project.components.getByName("release"))

            configureCommonPublicationParams(project, publication)
        }
    }

    private fun createJarMavenPublication(project: Project, publishing: PublishingExtension): MavenPublication {
        return publishing.publications.create("main", MavenPublication::class.java) { publication ->
            publication.artifact(getJavadocJarTask(project))
            publication.from(project.components.getByName("java"))

            configureCommonPublicationParams(project, publication)
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

    private fun getAndroidSourcesJarTask(project: Project): TaskProvider<Jar> {
        return project.tasks.register("sourcesJar", Jar::class.java) {
            it.from(getAndroidSourceSets(project).getByName("main").java.srcDirs)
            it.archiveClassifier.set("sources")
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

    private fun getAndroidSourceSets(project: Project): SourceSetContainer {
        val androidExtension = project.extensions.getByName("android")
        val type = MethodType.methodType(SourceSetContainer::class.java)
        val handler = MethodHandles.lookup().findVirtual(androidExtension.javaClass, "getSourceSets()", type)
        return handler.invoke() as SourceSetContainer
    }

    private fun configureCommonPublicationParams(project: Project, publication: MavenPublication) {
        publication.artifact(getSourcesJarTask(project))

        publication.groupId = extension.group.get()
        publication.version = extension.version.get()

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

    private fun applySubprojectPlugins(plugins: PluginContainer) {
        plugins.apply(MavenPublishPlugin::class.java)
        plugins.apply(SigningPlugin::class.java)
        plugins.apply(DokkaPlugin::class.java)
    }

    private fun applyRootProjectPlugins(plugins: PluginContainer) {
        plugins.apply(NexusPublishPlugin::class.java)
    }

    private fun createPublishToMavenCentralTask(project: Project) {
        val tasks = project.tasks
        val finishReleaseTask = tasks.named("closeAndReleaseSonatypeStagingRepository")

        project.subprojects { subProject ->
            subProject.tasks.configureEach {
                if (it.name == "publishToSonatype") {
                    finishReleaseTask.configure { releaseTask ->
                        releaseTask.dependsOn(it)
                    }
                }
            }
        }

        tasks.register("publishToMavenCentral") {
            it.dependsOn(finishReleaseTask)
        }
    }
}