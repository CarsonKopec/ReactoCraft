plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":protocol"))
    implementation(project(":plugins-api"))

    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("io.projectreactor:reactor-core:3.8.0-M5")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
