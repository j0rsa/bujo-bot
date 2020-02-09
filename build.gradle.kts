import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    kotlin("jvm") version Versions.kotlin
    id(Plugins.gitProperties) version Versions.gitProperties
    id(Plugins.dockerCompose) version Versions.dockerCompose
    id(Plugins.docker) version Versions.docker
    id(Plugins.shadow) version Versions.shadow
}

group = "com.j0rsa.bujo.telegram"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(Libs.telegramApi)
    implementation(Libs.http4kClient)
    implementation(Libs.http4kFormat)
    implementation(Libs.config4k)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = App.compileVersion
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = App.compileVersion
    }
}

val hash = Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.reader().use { it.readText() }.trim()
val projectTag = hash
val baseDockerName = "j0rsa/${project.name}"
val taggedDockerName = "$baseDockerName:$projectTag"

val baseDockerFile = file("$projectDir/Dockerfile")
docker {
    val shadowJar: ShadowJar by tasks
    name = taggedDockerName
    setDockerfile(baseDockerFile)
    tag("latest", taggedDockerName)
    buildArgs(mapOf("JAR_FILE" to shadowJar.archiveFileName.get()))
    files(shadowJar.outputs)
}
