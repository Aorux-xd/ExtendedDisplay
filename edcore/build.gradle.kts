plugins {
    id("java")
    id("com.gradleup.shadow")
    id("dev.ed.edprotect")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperVersion")}")
    compileOnly("com.velocitypowered:velocity-api:${property("velocityVersion")}")
    annotationProcessor("com.velocitypowered:velocity-api:${property("velocityVersion")}")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.yaml:snakeyaml:2.2")
}

edprotect {
    enabled = true
    strength = "maximum"
    stringEncryption = true
    controlFlow = true
    hardwareBinding = true
    anchorClass = "dev.ed.edcore.internal.EDCoreLifecycle"
    excludeClasses =
        "dev.ed.edcore.api.EDCoreAPI," +
        "dev.ed.edcore.api.EDCoreProvider," +
        "dev.ed.edcore.api.auth.EDAuthAPI," +
        "dev.ed.edcore.api.auth.AuthListener," +
        "dev.ed.edcore.api.auth.PlayerProfile," +
        "dev.ed.edcore.api.auth.event.AuthRegisterEvent," +
        "dev.ed.edcore.api.auth.event.AuthLoginEvent," +
        "dev.ed.edcore.api.auth.event.AuthLogoutEvent," +
        "dev.ed.edcore.api.auth.event.AuthPremiumChangeEvent," +
        "dev.ed.edcore.api.auth.event.AuthTOTPEnableEvent," +
        "dev.ed.edcore.api.auth.event.AuthTOTPDisableEvent," +
        "dev.ed.edcore.api.auth.event.AuthFailedLoginEvent," +
        "dev.ed.edcore.api.auth.event.AuthLoginMethod," +
        "dev.ed.edcore.api.gate.EdCoreGate," +
        "dev.ed.edcore.api.license.LicenseStatus"
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("project" to mapOf("version" to project.version))
        }
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
