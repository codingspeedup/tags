plugins {
    id("java")
    id("groovy")
    // id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "io.github.codingspeedup"
version = "2026-01-09"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.1")
        // testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
        // bundledPlugin("org.intellij.plugins.markdown")
    }

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("org.slf4j:slf4j-jdk14:2.0.9")

    val groovyVersion = "4.0.29"
    implementation("org.apache.groovy:groovy:$groovyVersion")
    implementation("org.apache.groovy:groovy-ant:$groovyVersion")

    val langchain4jVersion = "1.10.0"
    implementation("dev.langchain4j:langchain4j:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-azure-open-ai:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")

    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")

    implementation("com.github.javaparser:javaparser-core:3.27.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }

        java {
            srcDirs(emptyList<String>())
        }
        // Type-safe way to access Groovy in Kotlin DSL
        extensions.getByType<GroovySourceDirectorySet>().srcDirs("src/main/groovy", "src/main/java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "231"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<GroovyCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    test {
        useJUnitPlatform()
    }
}


// kotlin {
//    compilerOptions {
//        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
//    }
//     jvmToolchain(17)
// }
