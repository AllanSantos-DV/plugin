package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
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
    public StepResult execute(MergeContext context) {
        GitCommandResult mergeResult = service.merge(
                context.repository,
                context.sourceBranch,
                context.squash,
                context.commitMessage);
        if (mergeResult.success()) {
            context.successfulMerges.add(context.targetBranch);
            return StepResult.SUCCESS;
        }

        context.errorMessage = MessageBundle.message("error.merge", context.sourceBranch, context.targetBranch,
                String.join("\n", mergeResult.getErrorOutput()));

        return new ResultFailStep(mergeResult, context.errorMessage).checkConflict(context);
    }

    @Override
    public StepResult failure(MergeContext context) {
        context.allSuccessful = false;
        context.failedMerges.add(context.targetBranch);
        return StepResult.SKIPPED;
    }

    @Override
    public void success(MergeContext context) {
        context.successfulMerges.add(context.targetBranch);
    }
}