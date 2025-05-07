package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que retorna para a branch original após o merge.
 */
public class ReturnToOriginalBranchStep implements MergeStep {
    private final GitRepositoryOperations service;
    private final String originalBranch;

    public ReturnToOriginalBranchStep(GitRepositoryOperations service, String originalBranch) {
        this.service = service;
        this.originalBranch = originalBranch;
    }

    @Override
    public boolean execute(MergeContext context) {
        GitCommandResult returnResult = service.checkout(context.repository, originalBranch);
        if (!returnResult.success()) {
            NotificationHelper.notifyError(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.return", originalBranch,
                            String.join("\n", returnResult.getErrorOutput())));
            context.allSuccessful = false;
            // Não adiciona a branch à lista de falhas pois não é uma target
            return false;
        }
        return true;
    }
}