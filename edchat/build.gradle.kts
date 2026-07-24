plugins {
    id("java")
    id("dev.ed.edprotect")
}

dependencies {
    compileOnly(project(":edcore"))
    compileOnly("io.papermc.paper:paper-api:${property("paperVersion")}")
}

edprotect {
    enabled = true
    strength = "maximum"
    stringEncryption = true
    controlFlow = true
    hardwareBinding = true
    anchorClass = "dev.ed.edchat.EDChatPlugin"
    excludeClasses = ""
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("project" to mapOf("version" to project.version))
    }
}
