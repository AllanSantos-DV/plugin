plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.plugin"
version = "1.1.0"

repositories {
    mavenCentral()
}

// Configuração da versão do IntelliJ IDEA e dependências
intellij {
    version.set("2023.1")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf("Git4Idea"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("231") // IntelliJ IDEA 2023.1+
    }
    
    buildSearchableOptions {
        enabled = false
    }
    
    // Forçando o uso do Gradle 7.6
    wrapper {
        gradleVersion = "7.6.2"
        distributionType = Wrapper.DistributionType.ALL
    }
    
    // Configuração do nome do arquivo ZIP gerado
    buildPlugin {
        archiveBaseName.set("GitMultiMerge")
        archiveVersion.set(version)
        archiveClassifier.set("")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
} 