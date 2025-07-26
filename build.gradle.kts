import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish.base") version "0.34.0"
    id("com.gradle.plugin-publish") version "1.2.1"
}

description = "Internal plugin for publishing LikeTheSalad Java and Android libs"

val publishingCoordinates = Coordinates(
    "Artifact Publisher",
    "https://github.com/LikeTheSalad/artifact-publisher",
    "https://github.com/LikeTheSalad/artifact-publisher.git",
    "https://github.com/LikeTheSalad/artifact-publisher/issues"
)

data class Coordinates(
    val publicName: String,
    val url: String,
    val vcsUrl: String,
    val issueTrackerUrl: String
)

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

val javaVersion = JavaVersion.VERSION_11
val minKotlinVersion = KotlinVersion.KOTLIN_1_9

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        apiVersion = minKotlinVersion
        languageVersion = minKotlinVersion
    }
}

dependencies {
    api("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:8.3.8")
    implementation("com.gradle.publish:plugin-publish-plugin:1.3.1")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}

gradlePlugin {
    website.set(publishingCoordinates.url)
    vcsUrl.set(vcsUrl)
    plugins {
        create("publisher") {
            id = "com.likethesalad.artifact-publisher"
            displayName = publishingCoordinates.publicName
            description = project.description
            tags.addAll(listOf("publisher", "likethesalad"))
            implementationClass = "com.likethesalad.tools.artifact.publisher.ArtifactPublisherPlugin"
        }
    }
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withJavadocJar()
    withSourcesJar()
}

mavenPublishing {
    publishToMavenCentral(true)
}

publishing {
    publications {
        create("likethesalad", MavenPublication::class.java) {
            from(components.getByName("java"))

            pom {
                name = publishingCoordinates.publicName
                description = project.description
                url = publishingCoordinates.url
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "LikeTheSalad"
                        name = "Cesar Munoz"
                        email = "likethesalad@gmail.com"
                    }
                }
                scm {
                    url = publishingCoordinates.url
                    connection = publishingCoordinates.vcsUrl
                }
                issueManagement {
                    url = publishingCoordinates.issueTrackerUrl
                }
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("likethesalad"))
}