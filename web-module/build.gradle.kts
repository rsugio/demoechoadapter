plugins {
    war
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.war {
//    webXml = file("src/main/webapp/WEB-INF/web.xml")
}

dependencies {
    //providedCompile("javax.servlet:javax.servlet-api:3.1.0")
    providedCompile("javax.servlet:servlet-api:2.5")
    implementation("org.apache.wicket:wicket-core:7.18.0")
}

// Настройка source sets
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
        // webapp не входит в standard source set
    }
}
