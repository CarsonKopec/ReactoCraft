plugins {
    id("java")
    application
}

group = "reactocraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io/") }
}

dependencies {
    implementation(project(":server-core"))
}

application {
    mainClass.set("reactocraft.Main")
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf("-Djava.library.path=${rootDir}/native-worldgen/target/release")
}
