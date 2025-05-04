# Internacionalização do Git Multi Merge Plugin

Este documento explica como o plugin suporta múltiplos idiomas e como adicionar novos idiomas.

## Como Funciona a Internacionalização

O Git Multi Merge Plugin utiliza o sistema de internacionalização oficial do IntelliJ IDEA. Isso permite que textos, mensagens e elementos da interface sejam exibidos no idioma preferido do usuário.

### Estrutura de Arquivos

Os arquivos de tradução estão organizados assim:

```
src/main/resources/messages/
├── GitMultiMergeBundle.properties       # Padrão (Inglês)
├── GitMultiMergeBundle_pt_BR.properties # Português do Brasil
└── GitMultiMergeBundle_es.properties    # Espanhol
```

### Configuração no plugin.xml

A internacionalização é configurada no arquivo `plugin.xml` de duas formas:

1. **Definição do Resource Bundle**:
   ```xml
   <resource-bundle>messages.GitMultiMergeBundle</resource-bundle>
   ```

2. **Referência a chaves em actions**:
   ```xml
   <action id="GitMultiMerge.Action" 
           text="action.GitMultiMerge.text" 
           description="action.GitMultiMerge.description"
           icon="/icons/multiMerge.svg">
   ```

3. **Referência a chaves em componentes**:
   ```xml
   <notificationGroup id="Git Multi Merge" 
                     displayType="BALLOON" 
                     toolWindowId="Version Control" 
                     bundle="messages.GitMultiMergeBundle"
                     key="notification.group.git.multi.merge"/>
   ```

### Uso no Código

As mensagens localizadas são acessadas usando a classe `MessageBundle`:

```java
import com.plugin.gitmultimerge.util.MessageBundle;

// Texto simples
String message = MessageBundle.message("chave.da.mensagem");

// Texto com parâmetros
String formatted = MessageBundle.message("chave.com.parametro", param1, param2);

// Para otimizar performance (carregamento tardio)
Supplier<String> lazyMessage = MessageBundle.messagePointer("chave.da.mensagem");
```

## Como Adicionar um Novo Idioma

Para adicionar suporte a um novo idioma:

1. Crie um novo arquivo na pasta `src/main/resources/messages/` seguindo o padrão de nomenclatura:
   - `GitMultiMergeBundle_<código_do_idioma>.properties`
   - Exemplo: `GitMultiMergeBundle_fr.properties` para francês
   - Exemplo: `GitMultiMergeBundle_de.properties` para alemão

2. Traduza todas as mensagens do arquivo padrão (`GitMultiMergeBundle.properties`).

3. Mantenha todas as chaves idênticas, alterando apenas o texto traduzido.

### Exemplo de Arquivo de Tradução

```properties
# Chaves e valores em inglês (padrão)
dialog.title=Git Multi Merge
button.merge=Merge

# As mesmas chaves em português
dialog.title=Git Multi Merge
button.merge=Mesclar
```

## Formatação Especial

Para mensagens que utilizam parâmetros, use a sintaxe `{0}`, `{1}`, etc.:

```properties
notification.merge.success=Successfully merged {0} into {1}
```

Quando chamar no código:

```java
MessageBundle.message("notification.merge.success", sourceBranch, targetBranch);
```

## Idiomas Suportados Atualmente

- Inglês (padrão)
- Português do Brasil (pt_BR)
- Espanhol (es)

## Detecção do Idioma

O plugin detecta automaticamente o idioma da IDE do usuário e carrega os recursos correspondentes. Se o idioma não estiver disponível, o inglês (padrão) será usado.

## Requisitos de Configuração

Para que a internacionalização funcione corretamente, o arquivo `build.gradle.kts` deve conter:

```kotlin
intellij {
    // Outras configurações...
    
    // Necessário para internacionalização
    instrumentCode.set(false)
    downloadSources.set(true)
}

// Configuração do Java para UTF-8
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Processamento de recursos
tasks {
    processResources {
        from("src/main/resources") {
            include("**/*.properties")
        }
    }
}
```

## Contribuições

Contribuições de traduções para novos idiomas são bem-vindas! Por favor, siga as instruções acima e envie um Pull Request. 