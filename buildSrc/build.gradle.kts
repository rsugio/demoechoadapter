
plugins {
    id("java-library")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.rsug:komar:0.0.1")
//    implementation("org.ow2.asm:asm:9.6")
    implementation("commons-io:commons-io:2.22.0")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
}
