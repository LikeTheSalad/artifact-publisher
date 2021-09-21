package com.likethesalad.tools.artifact.publisher.tools

import java.io.File

object AndroidSourceSetsHelper {

    fun getAndroidSourceSets(kotlinExt: Any): Set<File> {
        return getAllSrcDirs(getMainSourceSet(kotlinExt))
    }

    private fun getMainSourceSet(extension: Any): Any {
        val getSourceSetsMethod = extension.javaClass.getDeclaredMethod("getSourceSets")
        val sourceSets = getSourceSetsMethod.invoke(extension)
        val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
        return getByNameMethod.invoke(sourceSets, "main")
    }

    @Suppress("UNCHECKED_CAST")
    private fun getAllSrcDirs(sourceSet: Any): Set<File> {
        val getKotlinMethod = sourceSet.javaClass.getMethod("getKotlin")
        val kotlin = getKotlinMethod.invoke(sourceSet)
        val getSrcDirsMethod = kotlin.javaClass.getDeclaredMethod("getSrcDirs")
        return getSrcDirsMethod.invoke(kotlin) as Set<File>
    }
}