package com.likethesalad.tools.artifact.publisher

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gradle.publish.PublishPlugin
import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherTargetExtension
import com.likethesalad.tools.artifact.publisher.extensions.ShadowExtension
import com.likethesalad.tools.artifact.publisher.publications.AarMavenPublicationCreator
import com.likethesalad.tools.artifact.publisher.publications.JarMavenPublicationCreator
import com.likethesalad.tools.artifact.publisher.publications.MavenPublicationCreator
import com.likethesalad.tools.artifact.publisher.tasks.ChangelogUpdaterTask
import com.likethesalad.tools.artifact.publisher.tools.DependencyInfo
import com.likethesalad.tools.artifact.publisher.tools.PomReader
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin

class ArtifactPublisherPlugin : Plugin<Project> {

    companion object {
        private const val EXTENSION_ARTIFACT_PUBLISHER_NAME = "artifactPublisher"
        private const val EXTENSION_ARTIFACT_PUBLISHER_TARGET_NAME = "artifactPublisherTarget"
        private const val EMBEDDED_CLASSPATH_CONFIG_NAME = "embeddedClasspath"
        private const val TASKS_GROUP = "publishing"
        const val GRADLE_PLUGIN_ID = "java-gradle-plugin"
    }

    private lateinit var extension: ArtifactPublisherExtension
    private lateinit var rootProject: Project

    override fun apply(project: Project) {
        verifyRootProject(project)
        this.rootProject = project
        applyRootProjectPlugins(project.plugins)
        extension = project.extensions.create(EXTENSION_ARTIFACT_PUBLISHER_NAME, ArtifactPublisherExtension::class.java)
        configureExtensionDefaults(project)

        project.subprojects { subProject ->
            configureSubproject(subProject)
        }

        configurePrePublishing(project)
        configurePublishing(project)
    }

    private fun configureExtensionDefaults(project: Project) {
        extension.description.set(project.provider { project.description })
        extension.version.set(project.provider { project.version.toString() })
        extension.group.set(project.provider { project.group.toString() })
    }

    private fun configureSubproject(subProject: Project) {
        val plugins = subProject.plugins
        val isRelease = isRelease(subProject)

        // For Java libraries
        plugins.withId("java-library") {
            configurePublishTarget(isRelease, subProject, JarMavenPublicationCreator(extension))
            val intransitiveConfiguration = createEmbeddedIntransitiveConfiguration(subProject)
            configureEmbeddedDependencies(intransitiveConfiguration, subProject)
            configureShadow(subProject, intransitiveConfiguration)
            plugins.withId(GRADLE_PLUGIN_ID) {
                configureShadowElements(subProject)
                if (isRelease) {
                    configureGradlePublishPlugin(subProject)
                }
            }
        }

        // For Android libraries
        plugins.withId("com.android.library") {
            configurePublishTarget(isRelease, subProject, AarMavenPublicationCreator(extension))
        }
    }

    private fun configurePublishTarget(
        isRelease: Boolean,
        subProject: Project,
        mavenPublicationCreator: MavenPublicationCreator
    ) {
        val plugins = subProject.plugins
        applySubprojectPlugins(plugins)
        if (isRelease) {
            addSigningPlugin(plugins)
        }
        setPropertiesFromExtension(subProject)
        val publishing = subProject.extensions.getByType(PublishingExtension::class.java)
        val targetExtension = createTargetExtensionIfNeeded(subProject)
        mavenPublicationCreator.prepare(subProject, isRelease)
        subProject.afterEvaluate {
            if (!targetExtension.disablePublishing.get()) {
                val mainPublication = mavenPublicationCreator.create(subProject, publishing)
                if (isRelease) {
                    signPublication(subProject, mainPublication)
                }
            }
        }
    }

    private fun isRelease(project: Project): Boolean {
        return project.findProperty("release")?.equals("true") ?: false
    }

    private fun createTargetExtensionIfNeeded(subProject: Project): ArtifactPublisherTargetExtension {
        val extensions = subProject.extensions
        return extensions.findByName(EXTENSION_ARTIFACT_PUBLISHER_TARGET_NAME)?.let {
            it as ArtifactPublisherTargetExtension
        } ?: extensions.create(
            EXTENSION_ARTIFACT_PUBLISHER_TARGET_NAME,
            ArtifactPublisherTargetExtension::class.java
        )
    }

    private fun configureEmbeddedDependencies(intransitiveConfiguration: Configuration, project: Project) {
        intransitiveConfiguration.allDependencies.whenObjectAdded {
            if (it is ProjectDependency) {
                val embeddedProject = it.dependencyProject
                embeddedProject.afterEvaluate {
                    embeddedProject.configurations.getByName("implementation").allDependencies.forEach { dep ->
                        project.dependencies.add("implementation", dep)
                    }
                }
                val targetExtension = createTargetExtensionIfNeeded(embeddedProject)
                log("Disabling plugin publishing for ${embeddedProject.name}")
                targetExtension.disablePublishing.set(true)
            } else if (it is ExternalDependency) {
                val module = it.module
                val result = project.dependencies.createArtifactResolutionQuery()
                    .forModule(module.group, module.name, it.version)
                    .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
                    .execute()
                result.resolvedComponents.forEach { component ->
                    component.getArtifacts(MavenPomArtifact::class.java).forEach { artifact ->
                        artifact as ResolvedArtifactResult
                        val reader = PomReader(artifact.file)
                        addDependencies(project, reader.getDependencies())
                    }
                }
            }
        }
    }

    private fun addDependencies(project: Project, dependencies: List<DependencyInfo>) {
        dependencies.forEach {
            project.dependencies.add(it.scope.configurationName, it.getNotation())
        }
    }

    private fun configureGradlePublishPlugin(project: Project) {
        project.plugins.apply(PublishPlugin::class.java)
        val gradlePluginExtension = project.extensions.getByType(GradlePluginDevelopmentExtension::class.java)
        gradlePluginExtension.website.set(extension.url)
        gradlePluginExtension.vcsUrl.set(extension.vcsUrl)
        gradlePluginExtension.plugins.whenObjectAdded {
            it.displayName = extension.displayName.get()
            it.description = rootProject.description
            it.tags.addAll(extension.tags.get())
        }
    }

    private fun configureShadow(project: Project, intransitiveConfiguration: Configuration) {
        project.plugins.apply(ShadowPlugin::class.java)
        val shadowExtension = project.extensions.create("shadowExtension", ShadowExtension::class.java)
        project.tasks.withType(ShadowJar::class.java) { shadowJar ->
            shadowJar.archiveClassifier.set("")
            shadowJar.configurations = listOf(intransitiveConfiguration)
            shadowJar.mustRunAfter("jar")
        }
        project.afterEvaluate {
            project.tasks.withType(ShadowJar::class.java) { shadowJar ->
                shadowExtension.relocations.forEach {
                    shadowJar.relocate(it.pattern.get(), it.destination.get())
                }
            }
        }
    }

    private fun configureShadowElements(project: Project) {
        project.configurations.getByName("shadowRuntimeElements")
            .extendsFrom(project.configurations.getByName("runtimeElements"))
    }

    private fun createEmbeddedIntransitiveConfiguration(subProject: Project): Configuration {
        val configurations = subProject.configurations
        val bucket = configurations.create("embedded")
        bucket.isCanBeConsumed = false
        bucket.isCanBeResolved = false
        val classpath = configurations.create(EMBEDDED_CLASSPATH_CONFIG_NAME) {
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
            it.isTransitive = false
            it.extendsFrom(bucket)
        }
        configurations.named("compileClasspath") {
            it.extendsFrom(classpath)
        }
        val testClasspath = configurations.create("testEmbeddedClasspath") {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false
            it.extendsFrom(bucket)
        }

        configurations.named("testRuntimeClasspath") {
            it.extendsFrom(testClasspath)
        }

        subProject.tasks.withType(PluginUnderTestMetadata::class.java) {
            it.pluginClasspath.from(testClasspath)
        }

        return classpath
    }

    private fun setPropertiesFromExtension(subProject: Project) {
        subProject.version = extension.version.get()
        subProject.group = extension.group.get()
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

    private fun addSigningPlugin(plugins: PluginContainer) {
        plugins.apply(SigningPlugin::class.java)
    }

    private fun applySubprojectPlugins(plugins: PluginContainer) {
        plugins.apply(MavenPublishPlugin::class.java)
        plugins.apply(DokkaPlugin::class.java)
    }

    private fun applyRootProjectPlugins(plugins: PluginContainer) {
        plugins.apply(NexusPublishPlugin::class.java)
    }

    private fun configurePrePublishing(project: Project) {
        project.tasks.register("updateChangelog", ChangelogUpdaterTask::class.java) {
            it.group = TASKS_GROUP
            it.version.set(extension.version)
        }
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
                if (subProjectTask.name == "publishLikethesaladPublicationToSonatypeRepository") {
                    closeTask.configure {
                        it.dependsOn(subProjectTask)
                    }
                }
            }
        }

        tasks.register("publishToMavenCentral") {
            it.group = TASKS_GROUP
            it.dependsOn(finishReleaseTask)
        }
    }

    private fun log(message: String) {
        rootProject.logger.lifecycle(message)
    }
}