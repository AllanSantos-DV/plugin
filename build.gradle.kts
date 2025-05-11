plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.plugin"
version = file("VERSION").readText().trim()

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.1")
    type.set("IC")                 // Empacota para Community (IC) → roda em IC e IU
    plugins.set(listOf("Git4Idea"))
    instrumentCode.set(false)
    downloadSources.set(true)
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("263.*")
    }

    buildSearchableOptions {
        enabled = false
    }

    wrapper {
        gradleVersion = "8.2"
        distributionType = Wrapper.DistributionType.ALL
    }
    
    // Configuração do nome do arquivo ZIP gerado
    buildPlugin {
        archiveBaseName.set("GitMultiMerge")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from("src/main/resources") {
            include("**/*.properties")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}