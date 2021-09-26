package com.likethesalad.tools.artifact.publisher.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

@Suppress("UnstableApiUsage")
open class ArtifactPublisherTargetExtension(objectFactory: ObjectFactory) {
    val disablePublishing: Property<Boolean> = objectFactory.property(Boolean::class.java)

    init {
        disablePublishing.convention(false)
    }
}