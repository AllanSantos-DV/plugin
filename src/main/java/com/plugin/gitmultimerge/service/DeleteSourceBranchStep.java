package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepositoryManager;

/**
 * Etapa que delete a branch source local e remota, se necessário.
 */
public class DeleteSourceBranchStep {
    private final GitRepositoryOperations service;
    private final String originalBranch;

    /**
     * Construtor padrão.
     *
     * @param service        Serviço de operações Git.
     * @param originalBranch Nome da branch original.
     */
    public DeleteSourceBranchStep(GitRepositoryOperations service, String originalBranch) {
        this.service = service;
        this.originalBranch = originalBranch;
    }

    public boolean execute(MergeContext context) {
        if (!(context.deleteSourceBranch && context.allSuccessful)) {
            return true; // Não remover
        }
        if (originalBranch.equals(context.sourceBranch)) {
            // Tentar checkout automático para a target
            GitCommandResult checkoutOk = service.checkout(context.repository, context.targetBranch);
            if (checkoutOk.success()) {
                // Forçar atualização do repositório no IntelliJ
                GitRepositoryManager.getInstance(context.project).updateRepository(context.repository.getRoot());
                NotificationHelper.notifyInfo(
                        context.project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("info.checkout.before.delete", context.targetBranch,
                                context.sourceBranch));
            } else {
                NotificationHelper.notifyWarning(
                        context.project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("error.checkout.before.delete", context.targetBranch,
                                context.sourceBranch));
                return false;
            }
        }
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.sourceBranch);
        GitCommandResult deleteResult = service.deleteBranch(context.repository, context.sourceBranch);
        if (!deleteResult.success()) {
            return notifyWarning(deleteResult, context, "error.delete.local");
        } else if (remoteBranch != null) {
            GitCommandResult deleteRemoteResult = service.deleteRemoteBranch(context.repository, remoteBranch);
            if (!deleteRemoteResult.success()) {
                return notifyWarning(deleteRemoteResult, context, "error.delete.remote");
            }
        }
        return true;
    }

    private boolean notifyWarning(GitCommandResult result, MergeContext context, String key) {
        NotificationHelper.notifyWarning(
                context.project,
                NotificationHelper.DEFAULT_TITLE,
                MessageBundle.message(key, context.sourceBranch,
                        String.join("\n", result.getErrorOutput())));
        return false;
    }
}