package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

class JarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun onPrepare(project: Project, isRelease: Boolean) {
        project.extensions.configure(JavaPluginExtension::class.java) {
            if (isRelease) {
                it.withSourcesJar()
                it.withJavadocJar()
            }
        }
    }

    override fun getComponentName(): String {
        return "java"
    }
}