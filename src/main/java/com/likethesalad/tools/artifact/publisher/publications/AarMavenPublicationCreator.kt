package com.likethesalad.tools.artifact.publisher.publications

import com.android.build.api.dsl.LibraryExtension
import com.likethesalad.tools.artifact.publisher.extensions.ArtifactPublisherExtension
import org.gradle.api.Project

class AarMavenPublicationCreator(extension: ArtifactPublisherExtension) : MavenPublicationCreator(extension) {

    override fun onPrepare(project: Project, isRelease: Boolean) {
        project.extensions.configure(LibraryExtension::class.java) {
            it.publishing {
                singleVariant(getComponentName()) {
                    if (isRelease) {
                        withJavadocJar()
                        withSourcesJar()
                    }
                }
            }
        }
    }

    override fun getComponentName(): String {
        return "release"
    }
}