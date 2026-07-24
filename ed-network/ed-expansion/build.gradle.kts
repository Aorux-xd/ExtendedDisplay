plugins {
    id("java")
    id("com.gradleup.shadow")
    id("dev.ed.edprotect")
}

dependencies {
    implementation(project(":ed-common"))
    compileOnly(project(":edcore"))
    compileOnly("io.papermc.paper:paper-api:${property("paperVersion")}")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

edprotect {
    enabled = true
    strength = "maximum"
    stringEncryption = true
    controlFlow = true
    hardwareBinding = true
    anchorClass = "dev.ed.expansion.EdExpansionPlugin"
    excludeClasses = ""
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("project" to mapOf("version" to project.version))
        }
    }

    shadowJar {
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
