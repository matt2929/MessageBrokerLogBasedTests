plugins {
    application
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    testImplementation("org.jetbrains.kotlinx:lincheck:2.34")

    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}