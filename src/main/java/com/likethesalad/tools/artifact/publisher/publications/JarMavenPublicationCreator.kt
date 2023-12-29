package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.ArtifactPublisherPlugin.Companion.GRADLE_PLUGIN_ID
import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

class JarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun prepare(project: Project, isRelease: Boolean) {
        if (isNotAGradlePluginProject(project)) {
            project.extensions.configure(JavaPluginExtension::class.java) {
                if (isRelease) {
                    it.withSourcesJar()
                    it.withJavadocJar()
                }
            }
        }
    }

    private fun isNotAGradlePluginProject(project: Project) = !project.plugins.hasPlugin(GRADLE_PLUGIN_ID)

    override fun getComponentName(): String {
        return "java"
    }
}