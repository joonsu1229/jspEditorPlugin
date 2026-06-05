plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "com.github.codex"
version = "0.1.6"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    intellijPlatform {
        local("D:/IntelliJ IDEA Community Edition")
    }
}

intellijPlatform {
    instrumentCode = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "222"
            untilBuild = provider { null }
        }
    }

    buildSearchableOptions = false
}

tasks {
    withType<JavaCompile> {
        options.release.set(11)
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("")
    }
}
