# Git Multi Merge Plugin

Plugin para IntelliJ IDEA que permite realizar o merge de uma branch source para múltiplas branches target em uma única operação.

## Funcionalidades

- Selecionar uma branch source e até 5 branches target
- Opção para squash commits durante o merge
- Opção para deletar a branch source após o merge bem-sucedido
- Visualização de progresso em tempo real
- Detecção e notificação de conflitos
- Identificação de problemas com hooks Git

## Requisitos

- IntelliJ IDEA Community 2023.1 ou superior
- Git instalado e configurado

## Instalação Manual

1. Clone este repositório
2. Execute o comando `./gradlew buildPlugin` (Linux/Mac) ou `gradlew.bat buildPlugin` (Windows)
3. Instale o plugin manualmente:
   - No IntelliJ IDEA, vá para Settings/Preferences > Plugins
   - Clique no ícone de engrenagem e selecione "Install Plugin from Disk..."
   - Navegue até o diretório `build/distributions` deste projeto
   - Selecione o arquivo ZIP gerado e clique em "OK"

## Como Usar

1. Abra um projeto com repositório Git no IntelliJ IDEA
2. No menu "Git", selecione a opção "Multi Merge..."
3. Selecione a branch source e as branches target (até 5)
4. Configure as opções desejadas (squash commits, deletar branch source)
5. Adicione uma mensagem de merge personalizada (opcional)
6. Clique em "OK" para iniciar o processo
7. Acompanhe o progresso na barra de status
8. Resolva conflitos, se necessário
9. Verifique o resultado na notificação final

## Limitações

- Suporta apenas branches locais
- Limite de 5 branches target por operação 