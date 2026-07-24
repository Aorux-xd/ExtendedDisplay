plugins {
    id("java")
    id("com.gradleup.shadow")
    id("dev.ed.edprotect")
}

dependencies {
    compileOnly(project(":edcore"))
    compileOnly("io.papermc.paper:paper-api:${property("paperVersion")}")
    compileOnly("com.velocitypowered:velocity-api:${property("velocityVersion")}")
    annotationProcessor("com.velocitypowered:velocity-api:${property("velocityVersion")}")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.h2database:h2:2.3.232")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.mysql:mysql-connector-j:9.1.0")
}

edprotect {
    enabled = true
    strength = "maximum"
    stringEncryption = true
    controlFlow = true
    hardwareBinding = true
    anchorClass = "dev.ed.edauth.bootstrap.EDAuthBootstrap"
    excludeClasses =
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
        "dev.ed.edcore.api.auth.event.AuthLoginMethod"
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "velocity-plugin.json")) {
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
