package com.likethesalad.tools.artifact.publisher.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class ArtifactPublisherExtension(objectFactory: ObjectFactory) {
    val displayName: Property<String> = objectFactory.property(String::class.java)
    val description: Property<String> = objectFactory.property(String::class.java)
    val group: Property<String> = objectFactory.property(String::class.java)
    val version: Property<String> = objectFactory.property(String::class.java)
    val url: Property<String> = objectFactory.property(String::class.java)
    val vcsUrl: Property<String> = objectFactory.property(String::class.java)
    val issueTrackerUrl: Property<String> = objectFactory.property(String::class.java)
}