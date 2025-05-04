package com.plugin.gitmultimerge;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

/**
 * Ação principal para o plugin Git Multi Merge.
 * Permite realizar o merge de uma branch source para múltiplas branches target.
 * Implementa DumbAware para garantir disponibilidade durante indexação.
 */
public class GitMultiMergeAction extends AnAction implements DumbAware {

    /**
     * Construtor padrão sem configuração de apresentação.
     * A apresentação será configurada no método update.
     */
    public GitMultiMergeAction() {
        // A apresentação será configurada no método update para compatibilidade com
        // 2025
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // Obtém o repositório Git atual
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);

        // Verifica se há um repositório Git válido
        if (repositoryManager.getRepositories().isEmpty()) {
            NotificationHelper.notifyError(
                    project,
                    NotificationHelper.DEFAULT_TITLE,
                    "Não foi possível encontrar um repositório Git válido");
            return;
        }

        // Obtém o repositório ativo ou o primeiro disponível
        GitRepository repository = repositoryManager.getRepositories().get(0);

        // Exibe o diálogo para seleção de branches
        GitMultiMergeDialog dialog = new GitMultiMergeDialog(project, repository);

        // O diálogo lidará com a execução dos merges ao clicar em OK
        dialog.showAndGet();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Configuração da apresentação conforme padrão 2025
        Presentation presentation = e.getPresentation();
        presentation.setText("Git Multi Merge", false); // Não usar mnemônico
        presentation.setDescription("Merge uma branch para múltiplos targets");

        // Ativa a ação apenas quando um projeto está aberto e tem Git
        Project project = e.getProject();
        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        // Verifica se há repositórios Git disponíveis
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        boolean hasGitRepositories = !repositoryManager.getRepositories().isEmpty();

        // Atualiza visibilidade e estado de habilitação
        presentation.setEnabledAndVisible(hasGitRepositories);
    }
}