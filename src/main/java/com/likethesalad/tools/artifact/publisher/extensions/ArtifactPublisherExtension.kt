package com.likethesalad.tools.artifact.publisher.extensions

import org.gradle.api.model.ObjectFactory

open class ArtifactPublisherExtension(objectFactory: ObjectFactory) {
    val displayName = objectFactory.property(String::class.java)
    val description = objectFactory.property(String::class.java)
    val group = objectFactory.property(String::class.java)
    val version = objectFactory.property(String::class.java)
    val url = objectFactory.property(String::class.java)
    val vcsUrl = objectFactory.property(String::class.java)
    val issueTrackerUrl = objectFactory.property(String::class.java)
}