plugins {
    java
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    id("net.researchgate.release") version "2.8.1"
}

release {
    tagTemplate = "\${version}"
}

description = "Atlas"

allprojects {
    group = "io.qameta.atlas"
    version = version
}

configure(subprojects) {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_15
        targetCompatibility = JavaVersion.VERSION_15
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
    }

    val sourceJar by tasks.creating(Jar::class) {
        from(sourceSets.getByName("main").allSource)
        archiveClassifier.set("sources")
    }

    val javadocJar by tasks.creating(Jar::class) {
        from(tasks.getByName("javadoc"))
        archiveClassifier.set("javadoc")
    }

    configure<PublishingExtension> {
        publications.create<MavenPublication>(project.name) {
            groupId = "io.qameta.atlas"
            version = version
            pom.packaging = "jar"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
        repositories {
            mavenLocal()
        }
    }

    tasks.withType(Javadoc::class) {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    dependencyManagement {
        dependencies {
            dependency("org.apache.commons:commons-lang3:3.7")

            dependency("org.seleniumhq.selenium:selenium-java:3.141.59")
            dependency("io.appium:java-client:6.1.0")
            dependency("io.github.bonigarcia:webdrivermanager:2.1.0")
            dependency("ru.yandex.qatools.matchers:webdriver-matchers:1.4.1")
            dependency("org.awaitility:awaitility:3.1.2")

            dependency("org.slf4j:slf4j-api:1.7.25")
            dependency("org.slf4j:slf4j-simple:1.7.25")

            dependency("org.hamcrest:hamcrest-all:1.3")
            dependency("org.assertj:assertj-core:3.6.2")
            dependency("org.mockito:mockito-core:3.2.4")
            dependency("junit:junit:4.12")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }
}