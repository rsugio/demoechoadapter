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

// Настройка WAR-плагина
tasks.war {
    archiveFileName.set("demo.echoadapter.war")
//    archiveBaseName.set("my-web-app")
//    archiveVersion.set("1.0.0")

//    // Исключить лишние зависимости
//    classpath(fileTree("lib") {
//        exclude("tomcat-*.jar")
//    })

    // Добавить дополнительные ресурсы
//    from("src/extra") {
//        include("*.properties")
//        into("WEB-INF/classes")
//    }

    // web.xml для Servlet 2.5 (опционально)
//    webXml = file("src/main/webapp/WEB-INF/web.xml")
}

// Зависимости
dependencies {
    //implementation(project(":jar-module-2"))

    // Provided scope (Tomcat/Java EE предоставляет)
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")
    providedCompile("javax.servlet.jsp:javax.servlet.jsp-api:2.3.3")
//    providedCompile("javax.el:javax.el-api:3.0.0")

//    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    // Тестовые зависимости
//    testImplementation("junit:junit:4.13.2")
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

// Gretty plugin для embedded сервера
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.gretty:gretty:4.0.3")
    }
}

//apply(plugin = "org.gretty")
//
//// Gretty настройки
//configure<org.akhikhl.gretty.GrettyExtension> {
//    httpPort = 8080
//    contextPath = "/myapp"
//    servletContainer = "tomcat9"  // или "jetty9"
//    debugPort = 5005
//    enableNaming = true
//}
