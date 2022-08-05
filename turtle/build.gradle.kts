@file:Suppress("UnstableApiUsage")

import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.7.10"
    id("com.vanniktech.maven.publish") version "0.20.0"
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")
apply(plugin = "org.jlleitschuh.gradle.ktlint-idea")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.7.10")
    testImplementation("io.mockk:mockk:1.12.5")
}

tasks.test {
    useJUnitPlatform()
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("docs/dokka"))

    dokkaSourceSets {
        configureEach {
            jdkVersion.set(8)

            includes.from("module.md")

            sourceLink {
                localDirectory.set(file("./"))
                remoteUrl.set(URL("https://github.com/lordcodes/turtle/blob/master/"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(propertyOrEmpty("Turtle_Signing_Key"), propertyOrEmpty("Turtle_Signing_Password"))
}

fun Project.propertyOrEmpty(name: String): String {
    val property = findProperty(name) as String?
    return property ?: environmentVariable(name)
}

fun environmentVariable(name: String) = System.getenv(name) ?: ""
