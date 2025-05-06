package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que deleta a branch source local e remota, se necessário.
 */
public class DeleteSourceBranchStep implements MergeStep {
    private final GitMultiMergeServiceImpl service;
    private final String originalBranch;

    public DeleteSourceBranchStep(GitMultiMergeServiceImpl service, String originalBranch) {
        this.service = service;
        this.originalBranch = originalBranch;
    }

    @Override
    public boolean execute(MergeContext context) {
        if (!context.deleteSourceBranch || !context.allSuccessful) {
            return true; // Não precisa deletar
        }
        if (originalBranch.equals(context.sourceBranch)) {
            NotificationHelper.notifyWarning(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.current.branch", context.sourceBranch));
            return true;
        }
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