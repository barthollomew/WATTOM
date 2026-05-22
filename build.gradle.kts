plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "io.wattom"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.14.19")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.14.19")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Xlint:all",
        "-Werror",
        "-Xlint:-missing-explicit-ctor",
        "-Xlint:-classfile"
    ))
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes(
            "Premain-Class"           to "io.wattom.agent.WattomAgent",
            "Agent-Class"             to "io.wattom.agent.WattomAgent",
            "Main-Class"              to "io.wattom.launcher.Launcher",
            "Can-Redefine-Classes"    to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
    minimize()
    exclude("net/bytebuddy/build/**")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-Djdk.attach.allowAttachSelf=true",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED"
    )
    dependsOn(tasks.shadowJar)
    systemProperty("wattom.jar",
        tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
}
