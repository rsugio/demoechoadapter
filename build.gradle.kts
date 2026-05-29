import java.io.FileInputStream
import java.util.Properties

plugins {
    id("java-library")
}

group = "demo"

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
version = configProps["adapterVersion"].toString() // версия должна быть числом, /AdapterTypeMetaData/@version это xs:integer

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
}

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

tasks.register("sapSdaFromWar", SdaFromWar::class.java) {
    dependsOn(":web-module:war")
    dcName = configProps["dcNameWeb"].toString()
    propertyXml.set(propertiesXmlFile)

    warFile.set(file("web-module/build/libs/web-module.war"))
    sdaFile.set(file("build/$adapterVendor~${dcName.get()}.sda"))
    doLast {
        buildSDA()
    }
}

tasks.register("sapSca", Sca::class.java) {
    propertyXml.set(propertiesXmlFile)

    sdaFiles.from(fileTree("build").matching {include("*.sda")})
    scaFile.set(file("p:/workspace/ZRSUGIO00_1.sca"))
    scaFile.set(file("build/ZRSUGIO00_1.sca"))
    doLast {
        buildSCA()
    }
}

tasks.register("sap") {
    dependsOn("sapRarFromJar", "sapSdaFromLibs", "sapSdaFromRar", "sapSdaFromWar")
}