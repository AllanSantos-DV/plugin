package com.plugin.gitmultimerge;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

/**
 * Ação principal para o plugin Git Multi Merge.
 * Permite realizar o merge de uma branch source para múltiplas branches target.
 */
public class GitMultiMergeAction extends AnAction {

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
        // Obtendo o repositório atual ou o primeiro disponível
        GitRepository repository;
        if (repositoryManager.getRepositories().isEmpty()) {
            VcsNotifier.getInstance(project).notifyError(
                    "Git Multi Merge",
                    "Não foi possível encontrar um repositório Git válido",
                    "");
            return;
        } else {
            repository = repositoryManager.getRepositories().get(0);
        }

        // Exibe o diálogo para seleção de branches
        GitMultiMergeDialog dialog = new GitMultiMergeDialog(project, repository);
        dialog.showAndGet();// O diálogo lidará com a execução dos merges
        // As operações são executadas dentro do diálogo ao clicar em OK
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Ativa ou desativa o item de menu com base na disponibilidade do Git
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}