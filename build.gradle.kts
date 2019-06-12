import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "se.zensum"
version = "1.0-SNAPSHOT"
description = "Receive incoming HTTP requests/webhooks and write them to a Kafka topic"

val kotlinVersion = rootProject.extra.get("kotlinVersion")
val shadowVersion = rootProject.extra.get("shadowVersion")
val coroutinesVersion = "1.1.1"
val jvmVersion = "1.8"
val ktorVersion = "1.1.2"
val gradleVersion = "5.2.1"
val jUnitVersion = "5.4.0"

buildscript {
    extra.set("kotlinVersion", "1.3.21")
    extra.set("shadowVersion", "4.0.4")
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra.get("kotlinVersion")}")
        classpath("com.github.jengelman.gradle.plugins:shadow:${extra.get("shadowVersion")}")
    }
}

plugins {
    id("com.google.cloud.tools.jib") version "1.0.0"
    java
    maven
    idea
}
apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "kotlin")

defaultTasks("run")

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://jitpack.io")
}

val integrationTestImplementation: Configuration by configurations.creating {
    extendsFrom(configurations["testImplementation"])
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

    // Junit
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")

    // Integration tests
    integrationTestImplementation("junit:junit:4.12")
    integrationTestImplementation("org.testcontainers:testcontainers:1.10.6")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.25")

    // Project specific dependencies (disabled by default)
    implementation("ch.vorburger:fswatch:1.1.0")
    implementation("com.github.zensum:webhook-proto:0.1.2")
    implementation("com.github.zensum:ktor-prometheus-feature:-SNAPSHOT")
    implementation("com.github.zensum:ktor-sentry-feature:fde5bc8f")
    implementation("com.github.zensum:ktor-jwt:5dc52cb")
    implementation("com.github.zensum:ktor-health-check:011a5a8")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.github.jonross:fauxflake:90abbcf5b6")
    implementation("com.github.mantono:pyttipanna:1.0.0")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation(group = "com.github.everit-org.json-schema", name = "org.everit.json.schema", version = "1.11.0")
    implementation(group = "redis.clients", name = "jedis", version = "3.0.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
    implementation("com.google.cloud:google-cloud-pubsub:1.62.0")
    implementation("org.apache.kafka:kafka-clients:0.11.0.3")
}

// Important: All classes containing test cases must match the
// the regex pattern "^.*Tests?$" to be picked up by the junit-gradle plugin.
sourceSets {
    main {
        java.srcDir("src/main/java")
        java.srcDir("src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
    test {
        java.srcDir("src/main/java")
        java.srcDir("src/test/kotlin")
    }
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        java.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
    }
}

jib {
    from {
        image = "openjdk:8-jdk"
    }
    to {
        image = "zensum/leia:" + System.getenv("CIRCLE_SHA1")
    }
}

tasks {
    withType(Jar::class) {
        manifest {
            attributes["Main-Class"] = "se.zensum.leia.MainKt"
        }
    }

    @Suppress("DEPRECATION")
    withType(ShadowJar::class) {
        baseName = "shadow"
        classifier = null
        version = null
    }

    withType(KotlinCompile::class) {
        sourceCompatibility = jvmVersion
        kotlinOptions {
            jvmTarget = jvmVersion
        }
    }

    withType(JavaCompile::class) {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
        options.isIncremental = true
        options.encoding = "UTF-8"
    }

    withType<Wrapper> {
        gradleVersion = gradleVersion
    }
}

val mainClassName = "se.zensum.leia.MainKt" //Important that "Kt" is appended to class name

task<JavaExec>("run") {
    main = mainClassName
    classpath = sourceSets["main"].runtimeClasspath
}

task<JavaExec>("debug") {
    debug = true
    environment["DEBUG"] = true
    main = mainClassName
    classpath = sourceSets["main"].runtimeClasspath
}

task<Test>("integrationTest") {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnit {
        includeCategories("leia.IntegrationTest")
    }
}
