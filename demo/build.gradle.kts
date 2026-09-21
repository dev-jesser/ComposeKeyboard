plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":composekeyboard"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.compose.ui.tooling.preview)
}

compose.desktop {
    application {
        mainClass = "io.github.devjesser.composekeyboard.demo.MainKt"
    }
}
