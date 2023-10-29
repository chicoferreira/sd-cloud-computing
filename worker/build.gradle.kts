plugins {
    id("java")
    id("application")
}

val main = "sd.cloudcomputing.worker.Main"

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
    implementation(files("libs/sd23.jar"))
    implementation ("commons-cli:commons-cli:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}