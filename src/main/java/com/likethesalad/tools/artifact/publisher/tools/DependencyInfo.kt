package com.likethesalad.tools.artifact.publisher.tools

data class DependencyInfo(val groupId: String, val artifactId: String, val version: String, val scope: Scope) {

    fun getNotation(): String {
        return "$groupId:$artifactId:$version"
    }

    enum class Scope(val configurationName: String) {
        RUNTIME("implementation"),
        COMPILE("api")
    }
}
