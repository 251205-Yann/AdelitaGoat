plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

group = "mx.edu.upchiapas.adelitagoat"
version = "1.0.0"

application {
    mainClass.set("mx.edu.upchiapas.adelitagoat.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.0.3"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")

    // PostgreSQL, HikariCP, Flyway
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")

    // Embedded PostgreSQL & H2 PostgreSQL compatibility fallback for local dev without Docker
    implementation("io.zonky.test:embedded-postgres:2.1.0")
    implementation("com.h2database:h2:2.3.232")

    // Security (BCrypt password hashing)
    implementation("org.mindrot:jbcrypt:0.4")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.0.21")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
