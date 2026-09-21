plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    `maven-publish`
}

group = "io.github.dev-jesser"
version = "0.1.0-SNAPSHOT"

kotlin {
    // Needs a JDK 17 installed (IntelliJ-downloaded JDKs are detected). Change if you use another.
    jvmToolchain(17)

    // Desktop (JVM) is the only target for now. The keyboard UI lives in commonMain so
    // other targets can be added later; the text-input integration lives in jvmMain.
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            implementation(libs.compose.animation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("ComposeKeyboard")
            description.set("An on-screen (soft) keyboard for Compose Multiplatform Desktop, built entirely in Compose.")
            url.set("https://github.com/dev-jesser/ComposeKeyboard")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("dev-jesser")
                    name.set("Jesse")
                }
            }
            scm {
                url.set("https://github.com/dev-jesser/ComposeKeyboard")
                connection.set("scm:git:https://github.com/dev-jesser/ComposeKeyboard.git")
            }
        }
    }

    // Optional internal repository (Nexus / GitLab package registry). Configure through
    // ~/.gradle/gradle.properties so no credentials live in the repo:
    //   composekeyboard.publishUrl=https://nexus.example.com/repository/maven-releases/
    //   composekeyboard.publishUser=...
    //   composekeyboard.publishPassword=...
    val publishUrl = providers.gradleProperty("composekeyboard.publishUrl").orNull
    if (publishUrl != null) {
        repositories {
            maven {
                name = "internal"
                url = uri(publishUrl)
                credentials {
                    username = providers.gradleProperty("composekeyboard.publishUser").orNull
                    password = providers.gradleProperty("composekeyboard.publishPassword").orNull
                }
            }
        }
    }
}
