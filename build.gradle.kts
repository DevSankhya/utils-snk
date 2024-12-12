plugins {
    id("java")
    id("application")
    `maven-publish`

}
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

group = "com.sankhya.ce"
// Use github tag variable
version = System.getenv("GITHUB_REF_NAME")?.toLowerCase()?.removePrefix("v")
    ?: System.getenv("VERSION")?.toLowerCase()
        ?.removePrefix("v") ?: getLatestTag().removePrefix("v")

fun getLatestTag(): String {
    val process = Runtime.getRuntime().exec("git describe --tags --abbrev=0")
    process.waitFor()
    val reader = process.inputStream.reader()
    val tag = "v" + reader.readText().trim()
    reader.close()
    process.destroy()
    // Sum 1 to the tag
    println(tag)
    val tagParts = tag.split(".").toMutableList()
    val lastPart = tagParts.last().toInt() + 1
    tagParts[tagParts.size - 1] = lastPart.toString()
    val newTag = tagParts.joinToString(".").replace("v", "")
    println(newTag)
    return newTag
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:20.1.0")
    implementation(fileTree("libs"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}



tasks {
    jar {
        archiveBaseName.set("utils-snk")
        archiveVersion.set("1.0.1")
        dependencies {
            implementation(fileTree("libs"))
        }
    }
}
