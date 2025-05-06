package com.plugin.gitmultimerge.command;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import com.plugin.gitmultimerge.ui.GitMultiMergeDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action responsible for opening the Git Multi Merge dialog in the IntelliJ UI.
 * Ensures the action is only available when a valid Git repository is present.
 */
public class GitMultiMergeAction extends AnAction implements DumbAware {

    /**
     * Default constructor.
     */
    public GitMultiMergeAction() {
        // Presentation is configured in the update method for compatibility.
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);

        if (repositoryManager.getRepositories().isEmpty()) {
            NotificationHelper.notifyError(
                    project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.no.git"));
            return;
        }

        GitRepository repository = repositoryManager.getRepositories().get(0);

        // Isolando a criação do diálogo para facilitar testes e extensões
        GitMultiMergeDialog dialog = createDialog(project, repository);
        dialog.showAndGet();
    }

    /**
     * Factory method for creating the dialog. Can be overridden for testing.
     */
    protected GitMultiMergeDialog createDialog(Project project, GitRepository repository) {
        return new GitMultiMergeDialog(project, repository);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText(MessageBundle.message("action.GitMultiMerge.text"), false);
        presentation.setDescription(MessageBundle.message("action.GitMultiMerge.description"));

        Project project = e.getProject();
        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        boolean hasGitRepositories = !repositoryManager.getRepositories().isEmpty();
        presentation.setEnabledAndVisible(hasGitRepositories);
    }
}