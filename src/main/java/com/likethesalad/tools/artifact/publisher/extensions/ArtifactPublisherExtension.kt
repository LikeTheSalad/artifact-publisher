package com.likethesalad.tools.artifact.publisher.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

open class ArtifactPublisherExtension(objectFactory: ObjectFactory) {
    val displayName: Property<String> = objectFactory.property(String::class.java)
    val description: Property<String> = objectFactory.property(String::class.java)
    val group: Property<String> = objectFactory.property(String::class.java)
    val version: Property<String> = objectFactory.property(String::class.java)
    val url: Property<String> = objectFactory.property(String::class.java)
    val vcsUrl: Property<String> = objectFactory.property(String::class.java)
    val issueTrackerUrl: Property<String> = objectFactory.property(String::class.java)
    val tags: SetProperty<String> = objectFactory.setProperty(String::class.java)

    init {
        tags.convention(emptyList())
    }
}