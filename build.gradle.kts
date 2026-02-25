plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
    kotlin("jvm") version "1.9.23"
}

group = "com.coinex.plugin"
version = "2.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2.1")
    type.set("IC") // IntelliJ Community 版，兼容 Android Studio
}

dependencies {
    implementation(kotlin("stdlib"))
}

sourceSets {
    main {
        java.srcDirs("src/main/java", "src/main/kotlin")
    }
    test {
        java.srcDirs("src/test/java", "src/test/kotlin")
    }
}

tasks {
    patchPluginXml {
        changeNotes.set("""
          more help：https://app.clickup.com/9008230771/v/dc/8cexcbk-190598
        """.trimIndent())

        sinceBuild.set("231")
        untilBuild.set("999.*")
    }
} 