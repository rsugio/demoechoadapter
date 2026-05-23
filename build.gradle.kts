import build.RarSdaBuilder

plugins {
    id("java")
}
val appVersion: String by project

group = "demo"
version = "0.1"

repositories {
    mavenLocal()
    mavenCentral()
}

java {
//    toolchain {
//        languageVersion = JavaLanguageVersion.of(8)
//    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val libsDirName = "libs"
dependencies {
    implementation(
        fileTree(
            mapOf(
                "dir" to libsDirName, "include" to listOf("*.jar")
            )
        )
    )
//    implementation("commons-io:commons-io:2.22.0")
//    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.google.code.gson:gson:2.13.2")
    //implementation("javax.resource:connector-api:1.5")    // см.connector.jar из ./libs

    //testImplementation("javax.resource:javax.resource-api:1.7.0")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("javax.xml.bind:jaxb-api:2.3.1")
    testImplementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    testImplementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
}

tasks.jar {
//    from(sourceSets.main.get().allSource) неудобно выводит

    from("src") {
        into("src")
    }

    from("build.gradle.kts") {
        into("src")
    }
    from("settings.gradle.kts") {
        into("src")
    }
    manifest {
        // attributes["Implementation-Version"] = "7.654321"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("buildRar") {
    dependsOn(tasks.jar)
    doLast {
        val constClassName = "demoecho.EchoAdapterConstants"
        val srcDir = layout.projectDirectory.file("src").asFile.toPath()
        val libsdir = layout.projectDirectory.file(libsDirName).asFile.toPath()
        val target = getLayout().getBuildDirectory().asFile.get().toPath()
        val jarfile = tasks.jar.get().archiveFile.get().asFile.toPath()
        // чтобы загрузить константы в сборщике, нужен класслоадер со всеми библиотеками, важно libsDirName
        val builder = RarSdaBuilder(
            version as String, constClassName, srcDir, jarfile, libsdir, target
        )
        builder.build()
    }
}

