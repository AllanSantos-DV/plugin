# Git Multi Merge Plugin

Plugin para IntelliJ IDEA que permite realizar o merge de uma branch source para múltiplas branches target simultaneamente, com opções de push automático e limpeza de branches.

## Funcionalidades

- Selecionar uma branch source e até 5 branches target para merge
- Interface otimizada com ComboBox para branch source e campo de busca para branches target
- Seleção automática da branch atual como source padrão
- Push automático para remotes após merge bem-sucedido
- Opção para squash commits durante o merge com commit automático
- Opção para deletar branch source (local e remota) após o merge bem-sucedido
- Atualização automática das informações do repositório com fetch e prune
- Visualização de progresso em tempo real com feedback detalhado
- Detecção e notificação de conflitos
- Identificação de problemas com hooks Git
- Notificação detalhada dos resultados das operações

## Requisitos

- IntelliJ IDEA 2023.1 ou superior (Community ou Ultimate)
- Git instalado e configurado
- Acesso aos remotes para operações de push e delete (se utilizadas)

## Instalação Manual

1. Clone este repositório
2. Execute o comando `./gradlew buildPlugin` (Linux/Mac) ou `gradlew.bat buildPlugin` (Windows)
3. Instale o plugin manualmente:
   - No IntelliJ IDEA, vá para Settings/Preferences > Plugins
   - Clique no ícone de engrenagem e selecione "Install Plugin from Disk..."
   - Navegue até o diretório `build/distributions` deste projeto
   - Selecione o arquivo ZIP gerado e clique em "OK"
   - Reinicie o IntelliJ IDEA quando solicitado

## Como Usar

1. Abra um projeto com repositório Git no IntelliJ IDEA
2. No menu "Git", selecione a opção "Multi Merge..."
3. Na tela de configuração:
   - A branch atual será pré-selecionada como source (você pode alterá-la no ComboBox)
   - Selecione as branches target (até 5) na lista à direita
   - Use o campo de busca para filtrar branches (útil em repositórios grandes)
4. Configure as opções desejadas:
   - **Squash commits**: Combina todos os commits da branch source em um único commit
   - **Push para remote após o merge**: Atualiza automaticamente o repositório remoto para cada branch após o merge
   - **Deletar branch source após o merge**: Remove a branch source (local e remota) depois que todos os merges forem bem-sucedidos
5. Adicione uma mensagem de merge personalizada (opcional)
6. Clique em "OK" para iniciar o processo
7. Acompanhe o progresso na barra de status
8. Resolva conflitos, se necessário
9. Verifique o resultado na notificação final

## Fluxo de trabalho completo do plugin

1. **Checkout** para cada branch target
2. **Merge** da branch source para a branch target
3. **Commit** automático após squash (se a opção estiver habilitada)
4. **Push** para o remote (se a opção estiver habilitada)
5. **Fetch com prune** para atualizar as informações do repositório
6. **Checkout** para uma branch segura
7. **Deleção** da branch source local (se solicitado e todos os merges forem bem-sucedidos)
8. **Deleção** da branch source remota (se existir, for solicitado e todos os merges forem bem-sucedidos)
9. **Notificação** do resultado com detalhes de cada operação

## Solução de problemas

- Se o plugin não aparecer no menu Git, verifique se está instalado e ativo em Settings > Plugins
- Certifique-se de estar em um projeto com Git configurado
- Em caso de erros persistentes, tente limpar o cache do IntelliJ (File > Invalidate Caches / Restart...)
- Verifique sua conexão com o remote antes de usar as funcionalidades de push/delete remotas
- Se as alterações não aparecerem no remote após o merge, verifique se a opção "Push para remote após o merge" está habilitada

## Limitações

- Suporta até 5 branches target por operação para evitar sobrecarga
- Os conflitos de merge precisam ser resolvidos manualmente
- As operações remotas exigem as permissões adequadas no repositório

## Contribuição

Sinta-se à vontade para contribuir com este projeto através de pull requests ou reportando problemas.

## Licença

Este plugin é distribuído sob a licença MIT.

## Contato

Para dúvidas ou sugestões, entre em contato com o desenvolvedor através do e-mail: [Allan Santos](mailto:allannascimentodossantos@gmail.com)
## Exemplo de uso

```java
    import com.intellij.openapi.actionSystem.AnAction;
    import com.intellij.openapi.actionSystem.AnActionEvent;
    import com.intellij.openapi.project.Project;
    import com.intellij.openapi.ui.Messages;
    import br.com.allandevs.gitmerge.actions.MultiMergeAction;
    import br.com.allandevs.gitmerge.model.MergeConfiguration;
    import br.com.allandevs.gitmerge.service.GitMultiMergeService;
    import java.util.Arrays;
    
    public class CustomMergeAction extends AnAction {
        @Override
        public void actionPerformed(AnActionEvent e) {
            Project project = e.getProject();
            if (project == null) return;
            
            // Configurando o merge programaticamente
            MergeConfiguration config = new MergeConfiguration();
            config.setSourceBranch("feature/nova-funcionalidade");
            config.setTargetBranches(Arrays.asList("develop", "release/1.0", "hotfix/urgent-fix"));
            config.setSquashCommits(true);
            config.setPushAfterMerge(true);
            config.setDeleteSourceBranch(false);
            config.setCommitMessage("Merge da feature para múltiplas branches");
            
            // Executando o merge
            GitMultiMergeService mergeService = project.getService(GitMultiMergeService.class);
            mergeService.executeMultiMerge(config, project, 
                success -> Messages.showInfoMessage("Merge concluído com sucesso!", "Multi Merge"),
                error -> Messages.showErrorDialog("Erro no merge: " + error, "Multi Merge Error")
            );
        }
    }
```