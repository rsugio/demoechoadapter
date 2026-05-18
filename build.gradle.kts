plugins {
    id("java")
}
val appVersion: String by project

group = "demo"
version = "0.0.1"

repositories {
    mavenCentral()
}

java {
//    toolchain {
//        languageVersion = JavaLanguageVersion.of(8)
//    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.jar")
            )
        )
    )
//    implementation("com.google.code.gson:gson:2.13.2")
    testImplementation("commons-io:commons-io:2.22.0")
    //implementation("javax.resource:javax.resource-api:1.7.0")
//    implementation("javax.resource:connector-api:1.5")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("javax.xml.bind:jaxb-api:2.3.1")
    testImplementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    testImplementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
}

tasks.jar {
    // default.jar переименовывается в колхозной сборке, здесь не меняем
    archiveFileName.set("default.jar")
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
        attributes["Implementation-Version"] = "7.654321"
    }
}

tasks.test {
    useJUnitPlatform()
}
