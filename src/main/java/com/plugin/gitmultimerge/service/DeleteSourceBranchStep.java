package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepositoryManager;

/**
 * Etapa que deleta a branch source local e remota, se necessário.
 */
public class DeleteSourceBranchStep implements MergeStep {
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

    @Override
    public boolean execute(MergeContext context) {
        if (!context.deleteSourceBranch || !context.allSuccessful) {
            return true; // Não precisa deletar
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
                return true;
            }
        }
        assert context.sourceBranch != null;
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.sourceBranch);
        GitCommandResult deleteResult = service.deleteBranch(context.repository, context.sourceBranch);
        if (!deleteResult.success()) {
            NotificationHelper.notifyWarning(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.delete.source", context.sourceBranch,
                            String.join("\n", deleteResult.getErrorOutput())));
        } else if (remoteBranch != null) {
            GitCommandResult deleteRemoteResult = service.deleteRemoteBranch(context.repository, remoteBranch);
            if (!deleteRemoteResult.success()) {
                NotificationHelper.notifyWarning(
                        context.project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("error.delete.remote", context.sourceBranch,
                                String.join("\n", deleteRemoteResult.getErrorOutput())));
            }
        }
        return true;
    }
}