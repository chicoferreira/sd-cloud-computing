plugins {
    id("java")
    id("application")
}

val main = "sd.cloudcomputing.client.Main"

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
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":common"))
    implementation("org.jline:jline-reader:3.24.0")
    implementation("org.jline:jline-terminal:3.24.0")
    implementation("org.jline:jline-terminal-jansi:3.24.0")
}

tasks.test {
    useJUnitPlatform()
}