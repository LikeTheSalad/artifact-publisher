package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.ArtifactPublisherPlugin.Companion.GRADLE_PLUGIN_ID
import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

class JarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun prepare(project: Project, isRelease: Boolean) {
        project.afterEvaluate {
            project.extensions.configure(JavaPluginExtension::class.java) {
                if (!project.plugins.hasPlugin(GRADLE_PLUGIN_ID)) {
                    if (isRelease) {
                        it.withSourcesJar()
                        it.withJavadocJar()
                    }
                    enableMavenCentralPublishing(project)
                }
            }
        }
    }

    override fun getComponentName(): String {
        return "java"
    }
}