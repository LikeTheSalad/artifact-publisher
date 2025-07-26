package com.likethesalad.tools.artifact.publisher.publications

import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project

class AarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun prepare(project: Project, isRelease: Boolean) {
        val androidExtension = project.extensions.findByName("android")!!

        val variantConfig: ((Any) -> Unit) = {
            if (isRelease) {
                val javaClass = it.javaClass
                javaClass.getMethod("withJavadocJar").invoke(it)
                javaClass.getMethod("withSourcesJar").invoke(it)
            }
        }

        val publishing = androidExtension.javaClass.getMethod("getPublishing").invoke(androidExtension)

        publishing.javaClass.getMethod("singleVariant", String::class.java, Function1::class.java)
            .invoke(publishing, getComponentName(), variantConfig)
        enableMavenCentralPublishing(project)
    }

    override fun getComponentName(): String {
        return "release"
    }
}