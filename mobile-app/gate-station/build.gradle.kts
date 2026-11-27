import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("com.tmc.mobile.gate.GateMainKt")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":fake-server"))
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<JavaExec>("run") {
    workingDir = project.projectDir
}

tasks.test {
    useJUnitPlatform()
}
