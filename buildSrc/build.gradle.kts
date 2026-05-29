
plugins {
    `java-gradle-plugin`
    `kotlin-dsl`  // если нужна поддержка Kotlin в buildSrc
}

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("sda-packer") {
            id = "io.rsug.sda-packer"
            implementationClass = "SdaBuildTask"
        }
    }
}

dependencies {
    // Добавляем зависимости при необходимости
    // implementation("com.sap:sap-sda-api:1.0")
    implementation("commons-io:commons-io:2.22.0")
    implementation("io.rsug:komar:0.0.1")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
}
