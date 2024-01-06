package com.likethesalad.tools.artifact.publisher.tools

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class PomReader(pomFile: File) {
    private val document: Document

    init {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        try {
            document = documentBuilderFactory.newDocumentBuilder().parse(pomFile)
            document.documentElement.normalize()
        } catch (e: SAXException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: ParserConfigurationException) {
            throw RuntimeException(e)
        }
    }

    fun getDependencies(): List<DependencyInfo> {
        val dependencies = mutableListOf<DependencyInfo>()
        val nodeList = document.getElementsByTagName("dependency")
        val length = nodeList.length
        if (length == 0) {
            return dependencies
        }

        var index = 0
        repeat(length) {
            dependencies.add(parseDependency(nodeList.item(index) as Element))
            index++
        }

        return dependencies
    }

    private fun parseDependency(item: Element): DependencyInfo {
        val groupId = extractItemValue(item, "groupId")
        val artifactId = extractItemValue(item, "artifactId")
        val version = extractItemValue(item, "version")
        val scope = extractItemValue(item, "scope")
        return DependencyInfo(
            groupId,
            artifactId,
            version,
            if (scope == "runtime") DependencyInfo.Scope.RUNTIME else DependencyInfo.Scope.COMPILE
        )
    }

    private fun extractItemValue(element: Element, itemName: String): String {
        return element.getElementsByTagName(itemName).item(0).textContent
    }
}