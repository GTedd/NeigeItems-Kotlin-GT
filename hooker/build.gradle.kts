import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":hooker-mythicmobs-v440"))
    implementation(project(":hooker-mythicmobs-v459"))
    implementation(project(":hooker-mythicmobs-v490"))
    implementation(project(":hooker-mythicmobs-v502"))
    implementation(project(":hooker-mythicmobs-v510"))
    implementation(project(":hooker-mythicmobs-v560"))

    implementation(project(":hooker-nms-NamespacedKey"))
    implementation(project(":hooker-nms-CustomModelData"))
    implementation(project(":hooker-nms-HoverEvent"))
}

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveClassifier.set("")
        exclude("META-INF/maven/**")
        exclude("META-INF/tf/**")
        exclude("module-info.java")
        // kotlin
        relocate("kotlin.", "pers.neige.neigeitems.libs.kotlin.")
        // stringsearcher
        relocate("org.neosearch.stringsearcher", "pers.neige.neigeitems.libs.stringsearcher")
        // javassist
        relocate("javassist", "pers.neige.neigeitems.libs.javassist")
        // maven-model
        relocate("org.codehaus.plexus.util", "pers.neige.neigeitems.libs.plexus.util")
        relocate("org.apache.maven.model", "pers.neige.neigeitems.libs.maven.model")
        // fastjson2
        relocate("com.alibaba.fastjson2", "pers.neige.neigeitems.libs.fastjson2")
    }
    kotlinSourcesJar {
        // include subprojects
        rootProject.subprojects.forEach { from(it.sourceSets["main"].allSource) }
    }
    build {
        dependsOn(shadowJar)
    }
}

fun final() {
    val mainFile =
        File("${rootProject.buildDir}/libs/${rootProject.name}-${rootProject.property("version")}-shaded.jar")
    val newMainFile =
        File("${rootProject.buildDir}/libs/${rootProject.name}-${rootProject.property("version")}.jar")
    val currentFile = File("${project.buildDir}/libs/${project.name}-${project.property("version")}.jar")

    if (!mainFile.exists()) return
    if (!currentFile.exists()) return

    JarOutputStream(FileOutputStream(newMainFile)).use { jarOutputStream ->
        JarFile(mainFile).use { jarFile ->
            val entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()

                jarOutputStream.putNextEntry(entry)
                jarFile.getInputStream(entry).copyTo(jarOutputStream)
                jarOutputStream.closeEntry()
            }
        }
        JarFile(currentFile).use { jarFile ->
            val entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryName = entry.name
                if (entryName.endsWith(".class")) {
                    jarOutputStream.putNextEntry(entry)
                    jarFile.getInputStream(entry).copyTo(jarOutputStream)
                    jarOutputStream.closeEntry()
                }
            }
        }
    }

    mainFile.delete()
}

val finalTask = tasks.register("finalTask") {
    doLast {
        final()
    }
}

tasks.getByName("build").finalizedBy(finalTask)
