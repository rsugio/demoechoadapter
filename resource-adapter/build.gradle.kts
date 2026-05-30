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
    implementation(
        fileTree(
            mapOf(
                "dir" to "../libs", "include" to listOf("*.jar")
            )
        )
    )
//    implementation("com.google.code.gson:gson:2.13.2")
//    implementation("org.apache.commons:commons-lang3:3.20.0")
//    implementation("commons-io:commons-io:2.22.0")
    implementation("javax.resource:connector-api:1.5")    // см.connector.jar из ./libs

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
