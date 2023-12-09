plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "moe.nea"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktor_version="2.3.7"
dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("dev.kord", "kord-core", "0.12.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

application {
    mainClass.set("moe.nea.gaybot.GaybotKt")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}