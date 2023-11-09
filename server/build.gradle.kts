plugins {
    id("java")
    id("application")
}

val main = "sd.cloudcomputing.server.Main"

application {
    mainClass.set(main)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = main
    }
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
    implementation(project(":common"))
}

tasks.test {
    useJUnitPlatform()
}