package com.likethesalad.tools.artifact.publisher.tools

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency

class DependenciesAppender(
    pomXml: Node,
    private val embedded: Set<Dependency>
) {
    private val existingDependenciesId = mutableListOf<String>()
    private val dependencies: Node

    companion object {
        private const val KEY_GROUP_ID = "groupId"
        private const val KEY_ARTIFACT_ID = "artifactId"
    }

    init {
        val existingDependencies = pomXml.get("dependencies") as? NodeList
        dependencies =
            if (existingDependencies!!.isNotEmpty()) existingDependencies.first() as Node else pomXml.appendNode(
                "dependencies"
            ) as Node
        val deps = dependencies.get("dependency") as? NodeList
        deps?.forEach {
            val dependencyId = getDependencyId(it as Node)
            existingDependenciesId.add(dependencyId)
        }
    }

    fun addSubprojectDependencies(subProjectDependency: Project) {
        subProjectDependency.configurations.getByName("runtimeClasspath").allDependencies.forEach {
            tryAddingDependency(it)
        }
    }

    fun tryAddingDependency(dependency: Dependency) {
        if (shouldAdd(dependency)) {
            addDependency(dependency)
        }
    }

    private fun shouldAdd(dependency: Dependency): Boolean {
        if (dependency in embedded) {
            return false
        }
        if (dependency is SelfResolvingDependency && dependency !is ProjectDependency) {
            return false
        }
        val id = getDependencyId(dependency)
        return id !in existingDependenciesId
    }


    private fun addDependency(dependency: Dependency) {
        val id = getDependencyId(dependency)
        val dependencyNode = dependencies.appendNode("dependency")

        dependencyNode.appendNode(KEY_GROUP_ID, dependency.group)
        dependencyNode.appendNode(KEY_ARTIFACT_ID, dependency.name)
        dependencyNode.appendNode("version", dependency.version)
        dependencyNode.appendNode("scope", "runtime")

        existingDependenciesId.add(id)
    }

    private fun getDependencyId(dependency: Dependency) = "${dependency.group}:${dependency.name}"

    private fun getDependencyId(node: Node): String {
        return "${getNodeItemValue(node, KEY_GROUP_ID)}:${getNodeItemValue(node, KEY_ARTIFACT_ID)}"
    }

    private fun getNodeItemValue(node: Node, itemName: String): String {
        val item = node.get(itemName) as NodeList
        return (item.first() as Node).text()
    }
}