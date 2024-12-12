import java.net.URL

plugins {
    id("java")
    id("application")
    `maven-publish`

}

val skwVersion = "master"
group = "br.com.sankhya.ce"

val archiveBaseName by extra("utils-snk")
// Propriedades customizadas(arquivo "gradle.properties")

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/devsankhya/utils-snk")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

// Use github tag variable
version = System.getenv("GITHUB_REF_NAME")?.toLowerCase()?.removePrefix("v") ?: System.getenv("VERSION")?.toLowerCase()
    ?.removePrefix("v") ?: getLatestTag().removePrefix("v")

fun getLatestTag(): String {
    val process = Runtime.getRuntime().exec("git describe --tags --abbrev=0")
    process.waitFor()
    val reader = process.inputStream.reader()
    val tag = "v" + reader.readText().trim()
    reader.close()
    process.destroy()
    // Sum 1 to the tag
    val tagParts = tag.split(".").toMutableList()
    val lastPart = tagParts.last().toInt() + 1
    tagParts[tagParts.size - 1] = lastPart.toString()
    val newTag = tagParts.joinToString(".").replace("v", "")
    return newTag
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus-repository.sankhya.com.br/repository/maven-public/")
    }
    maven {
        url = uri("https://nexus-repository.sankhya.com.br/repository/maven-devcenter-releases")
    }
    maven {
        url = uri("https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/")
    }
    maven {
        url = uri("https://maven.oracle.com")
    }


}

dependencies {
    implementation("br.com.sankhya", "mge-modelcore", skwVersion)
    implementation("br.com.sankhya", "jape", skwVersion)
    implementation("br.com.sankhya", "dwf", skwVersion)
    implementation("br.com.sankhya", "sanws", skwVersion)
    implementation("br.com.sankhya", "mge-param", skwVersion)
    implementation("br.com.sankhya", "skw-environment", skwVersion)
    implementation("br.com.sankhya", "sanutil", skwVersion)
    implementation("br.com.sankhya", "cuckoo", skwVersion)
    implementation("br.com.sankhya", "mgecom-model", skwVersion)
    implementation("br.com.sankhya", "mgefin-model", skwVersion)
    implementation("org.json", "json", "20240303")

    // Status HTTP / Apoio as Servlets
    implementation("com.squareup.okhttp3:okhttp:3.9.0")
    // https://mvnrepository.com/artifact/com.squareup.okio/okio
    implementation("com.squareup.okio:okio:1.13.0")
    implementation("org.jetbrains:annotations:24.0.0")

    // Manipulador de JSON
    implementation("com.google.code.gson", "gson", "2.1")

    // EJB / Escrever no container wildfly
    implementation("org.wildfly:wildfly-spec-api:16.0.0.Final")
//    implementation("org.jdom", "jdom", "1.1.3")
//    implementation("com.oracle.database.jdbc:ojdbc8:19.11.0.0")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}



tasks {
    val archiveBaseName: String by extra
    jar {
        this.archiveBaseName.set(archiveBaseName)
        archiveVersion.set(project.version.toString())
    }
}