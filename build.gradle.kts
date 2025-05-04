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
    
    // Necessário para internacionalização
    instrumentCode.set(false)
    downloadSources.set(true)
}

tasks {
    patchPluginXml {
        sinceBuild.set("231") // IntelliJ IDEA 2023.1+
        untilBuild.set("263.*") // Compatível com versões até 2026.3+
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
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
    }
    
    // Configurando o processamento de recursos para internacionalização
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        
        // Garantir que os arquivos de propriedades sejam copiados
        from("src/main/resources") {
            include("**/*.properties")
        }
    }
}

// Configuração do Java para UTF-8
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Configurações para Java 11
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

dependencies {
    testImplementation("junit:junit:4.13.2")
} 