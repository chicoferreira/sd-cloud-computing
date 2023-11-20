import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "sd-grupo-1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jline:jline-reader:3.24.0")
    implementation("org.jline:jline-terminal:3.24.0")
}

tasks {
    build {
        dependsOn(named<ShadowJar>("shadowJar"))
    }
}

tasks.test {
    useJUnitPlatform()
}