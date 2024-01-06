package com.likethesalad.tools.artifact.publisher.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class ShadowExtension @Inject constructor(objectFactory: ObjectFactory) {
    private val objectFactory: ObjectFactory
    internal val relocations = mutableListOf<Relocation>()

    init {
        this.objectFactory = objectFactory
    }

    fun relocate(pattern: String, destination: String) {
        val relocation = objectFactory.newInstance(Relocation::class.java)
        relocation.pattern.set(pattern)
        relocation.destination.set(destination)
        relocations.add(relocation)
    }

    interface Relocation {
        val pattern: Property<String>
        val destination: Property<String>
    }
}