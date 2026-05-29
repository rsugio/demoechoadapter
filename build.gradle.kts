import java.io.FileInputStream
import java.util.Properties

plugins {
    id("java-library")
}

group = "demo"
version = "1"

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
                "dir" to "libs", "include" to listOf("*.jar")
            )
        )
    )
//    implementation("javax.xml.bind:jaxb-api:2.3.1")
//    implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
//    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
}

//buildscript {
//    dependencies {
//        classpath(files("sda-builder/build/classes/java/main"))
//    }
//}

val propertiesXmlFile = file("properties.xml")
if (!propertiesXmlFile.exists())
    error("properties.xml not found, please run EchoAdapterConstants.main() manually before")
else
    println("properties.xml is good")

val configProps = Properties().apply {
    FileInputStream(propertiesXmlFile).use { inputStream ->
        loadFromXML(inputStream)
    }
}
val adapterVendor = configProps["adapterVendor"]

tasks.register("sapSdaFromLibs", SdaFromLibs::class.java) {
    propertyXml.set(propertiesXmlFile)
    dcName = configProps["dcNameLib"].toString()

    sdaFile.set(file("build/$adapterVendor~${dcName.get()}.sda"))
    providedLibs.from(
        project.file("libs/commons-io-2.22.0.jar"),
        project.file("libs/commons-lang3-3.20.0.jar")
    )
    doLast {
        buildSDA()
    }
}

tasks.register("sapRarFromJar", RarFromJar::class.java) {
    dependsOn(":resource-adapter:jar")
    dcName = configProps["dcNameRA"].toString()
    propertyXml.set(propertiesXmlFile)

    jarFile.set(file("resource-adapter/build/libs/resource-adapter.jar"))
    rarFile.set(file("build/$adapterVendor~${dcName.get()}.rar"))
    doLast {
        buildRAR()
    }
}

tasks.register("sapSdaFromRar", SdaFromRar::class.java) {
    dependsOn("sapRarFromJar", "sapSdaFromLibs")
    dcName = configProps["dcNameRA"].toString()
    propertyXml.set(propertiesXmlFile)

    rarFile.set(file("build/$adapterVendor~${dcName.get()}.rar"))
    sdaFile.set(file("build/$adapterVendor~${dcName.get()}.sda"))
    doLast {
        buildSDA()
    }
}


/*

    dependsOn(":web-module:build")

 */