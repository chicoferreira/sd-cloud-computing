plugins {
    id("java")
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
}

tasks.test {
    useJUnitPlatform()
}