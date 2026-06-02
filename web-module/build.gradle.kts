
plugins {
    war
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

sourceSets {
    main {
        resources {
            srcDir("src/main/java")
            include("**/*.html")
        }
    }
}

dependencies {
    providedCompile("javax.servlet:servlet-api:2.5")
    implementation("org.apache.wicket:wicket-core:6.30.0")

    // в основном чтение пропертей
    providedCompile(
        fileTree(
            mapOf(
                "dir" to "../libs", "include" to listOf("*.jar")
            )
        )
    )
}
