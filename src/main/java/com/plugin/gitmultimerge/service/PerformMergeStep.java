package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que executa o merge da branch source para a target.
 */
public class PerformMergeStep implements MergeStep {
    private final GitRepositoryOperations service;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public PerformMergeStep(GitRepositoryOperations service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        assert context.sourceBranch != null;
        GitCommandResult mergeResult = service.merge(
                context.repository,
                context.sourceBranch,
                context.squash,
                context.commitMessage);
        if (!mergeResult.success()) {
            NotificationHelper.notifyError(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.merge", context.sourceBranch, context.targetBranch,
                            String.join("\n", mergeResult.getErrorOutput())));
            context.allSuccessful = false;
            context.failedMerges.add(context.targetBranch);
            return false;
        }
        context.successfulMerges.add(context.targetBranch);
        return true;
    }
}