plugins {
    id("java")
    id("com.gradleup.shadow")
    id("dev.ed.edprotect")
}

dependencies {
    implementation(project(":ed-common"))
    compileOnly(project(":edcore"))
    compileOnly("com.velocitypowered:velocity-api:${property("velocityVersion")}")
    annotationProcessor("com.velocitypowered:velocity-api:${property("velocityVersion")}")
    implementation("org.yaml:snakeyaml:2.2")
}

edprotect {
    enabled = true
    strength = "maximum"
    stringEncryption = true
    controlFlow = true
    hardwareBinding = true
    anchorClass = "dev.ed.proxy.EdProxyPlugin"
    excludeClasses = ""
}

tasks {
    processResources {
        filesMatching("velocity-plugin.json") {
            expand("version" to project.version)
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
