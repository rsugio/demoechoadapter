plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("commons-io:commons-io:2.22.0")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation(
        fileTree(
            mapOf(
                "dir" to "../libs", "include" to listOf("*.jar")
            )
        )
    )
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    implementation("org.opentest4j:opentest4j:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    archiveFileName.set("../../../build/IA_Demo.jar")

    // Основные .class файлы
    from(sourceSets.main.get().output)

    // Исходники из src/main в папку /src/main/
    from(sourceSets.main.get().allSource) {
        into("src/main")
    }

    // Исходники из src/test в папку /src/test/
    from(sourceSets.test.get().allSource) {
        into("src/test")
    }

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
            //"Build-Date" to java.time.LocalDate.now().toString()
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
