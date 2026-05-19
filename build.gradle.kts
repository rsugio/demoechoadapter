import build.RarBuilder
import java.net.URLClassLoader

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
    //archiveFileName.set("default.jar")
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

tasks.register("buildRar") {
    dependsOn(tasks.jar)
    doLast {
        val classesDir = getLayout().getBuildDirectory().file("classes/java/main").get().asFile
        val constClassFile = File(classesDir, "demoecho/EchoAdapterConstants.class")
        if (!constClassFile.exists()) {
            throw RuntimeException("demoecho/EchoAdapterConstants.class not found! Run main project build first.")
        }
        val url = classesDir.toURI().toURL()
        val result = mutableMapOf<String, String>()
        URLClassLoader.newInstance(arrayOf(url), ClassLoader.getSystemClassLoader()).use { cl ->
            val constClass = cl.loadClass("demoecho.EchoAdapterConstants")
            constClass.declaredFields.forEach {field ->
                field.isAccessible = true
                val value = field.get(null)  // null для статических полей
                result[field.name] = value?.toString() ?: "null"
            }
        }
        val jarfile = tasks.jar.get().archiveFile.get().asFile.toPath()
//        val jarfile = getLayout().getBuildDirectory().file("default.jar").get().asFile.toPath()
        val target = getLayout().getBuildDirectory().asFile.get().toPath()
        val builder = RarBuilder(version as String, result as Map<String,String>, jarfile, target)
        builder.build()
    }
}

