import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springBootVersion = "3.4.1"
val junitPlatformVersion = "1.10.2"
val jsonPathVersion = "2.9.0" // override transitive dep due to CVE violation
val embeddedRedisVersion = "1.4.3"
val commonsValidatorVersion = "1.9.0"
val mockkVersion = "1.13.13"
val archunitVersion = "1.3.0"
val logunitVersion = "2.0.0"
val xmlunitVersion = "2.10.0"

plugins {
    val springDependencyManagementVersion = "1.1.7"
    val kotlinVersion = "2.1.0"
    val adarshrTestLoggerVersion = "4.0.0"
    val sonarqubeVersion = "6.0.1.5171"
    val gradleReleasePluginVersion = "3.1.0"
    val gradleNexusPublishPluginVersion = "2.0.0"

    kotlin("jvm") version kotlinVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
    id("java-library")
    id("com.adarshr.test-logger") version adarshrTestLoggerVersion
    id("jacoco")
    id("org.sonarqube") version sonarqubeVersion
    id("maven-publish")
    id("signing")
    id("net.researchgate.release") version gradleReleasePluginVersion
    id("io.github.gradle-nexus.publish-plugin") version gradleNexusPublishPluginVersion
}

group = "io.github.tobi-laa"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

sonar {
    properties {
        property("sonar.projectKey", "tobi-laa_spring-boot-embedded-redis")
        property("sonar.organization", "tobi-laa")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    api("com.github.codemonstur:embedded-redis:$embeddedRedisVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-test")
    implementation("org.springframework:spring-test")
    implementation("org.slf4j:slf4j-api")
    implementation("redis.clients:jedis")
    implementation("commons-validator:commons-validator:$commonsValidatorVersion")
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("io.github.netmikey.logunit:logunit-logback:$logunitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.xmlunit:xmlunit-core:$xmlunitVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonPathVersion") // override transitive dep due to CVE violation
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testImplementation("io.github.netmikey.logunit:logunit-core:$logunitVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    exclude("**/RedisValidationExtensionTest$*")
    exclude("**/RedisParameterResolverTest$*")
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username = System.getenv("OSSRH_USERNAME")
            password = System.getenv("OSSRH_TOKEN")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${group}"
            artifactId = rootProject.name
            version = version
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "${group}:${rootProject.name}"
                description = "Integrates embedded-redis with Spring Boot"
                url = "https://github.com/tobi-laa/spring-boot-embedded-redis.git"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                        comments = "A business-friendly OSS license"
                    }
                }
                developers {
                    developer {
                        id = "tobi-laa"
                        name = "Tobias Laatsch"
                        email = "tobias.laatsch@posteo.de"
                        organizationUrl = "https://github.com/tobi-laa"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:tobi-laa/spring-boot-embedded-redis.git"
                    developerConnection = "scm:git:git@github.com:tobi-laa/spring-boot-embedded-redis.git"
                    url = "https://github.com/tobi-laa/spring-boot-embedded-redis/tree/master"
                    tag = "HEAD"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/tobi-laa/spring-boot-embedded-redis/issues"
                }
                inceptionYear = "2024"
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
}

release {
    tagTemplate = "v${version}"
}
