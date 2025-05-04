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
        // Construtor vazio - a apresentação será configurada no método update
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
        // Configuração da apresentação - feita a cada atualização da interface
        Presentation presentation = e.getPresentation();
        presentation.setText("Git Multi Merge");
        presentation.setDescription("Merge uma branch para múltiplos targets");

        // Ativa ou desativa o item de menu com base na disponibilidade do Git
        Project project = e.getProject();
        presentation.setEnabledAndVisible(project != null);
    }
}