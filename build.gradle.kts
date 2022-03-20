import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.minmusxminmus"
version = "1.0"

repositories {
    mavenCentral()
    maven {
        name = "m2-dv8tion"
        url = uri("https://m2.dv8tion.net/releases")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))

    implementation(group = "net.dv8tion", name = "JDA", version = "4.3.0_277")
    implementation(group = "org.apache.logging.log4j", name = "log4j-slf4j-impl", version = "2.9.0")
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "2.7.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}