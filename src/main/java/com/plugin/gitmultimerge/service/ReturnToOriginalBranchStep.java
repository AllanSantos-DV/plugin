package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que retorna para a branch original após o merge.
 */
public class ReturnToOriginalBranchStep {
    private final GitRepositoryOperations service;
    private final String originalBranch;

    /**
     * Construtor padrão.
     *
     * @param service        Serviço de operações Git.
     * @param originalBranch Nome da branch original.
     */
    public ReturnToOriginalBranchStep(GitRepositoryOperations service, String originalBranch) {
        this.service = service;
        this.originalBranch = originalBranch;
    }

    public void execute(MergeContext context) {
        GitCommandResult returnResult = service.checkout(context.repository, originalBranch);
        if (!returnResult.success()) {
            NotificationHelper.notifyError(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.return", originalBranch,
                            String.join("\n", returnResult.getErrorOutput())));
        }

        // Atualiza o ChangeListManager ao final do fluxo
        UpdateChangeListManagerStep.update(context.project);
    }
}