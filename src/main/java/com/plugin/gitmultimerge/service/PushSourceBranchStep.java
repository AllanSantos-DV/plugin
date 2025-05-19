package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.exception.MultiMergeOperationException;

public class PushSourceBranchStep {
    private final GitRepositoryOperations service;

    public PushSourceBranchStep(GitRepositoryOperations service) {
        this.service = service;
    }

    public void execute(MergeContext context) throws MultiMergeOperationException {
        StepResult result = new PushBranchStep(service, context.sourceBranch, context.deleteSourceBranch)
                .execute(context);

        if (result == StepResult.CONFLICT) {
            boolean conflictResolved = new MergeConflictResolutionStep(service).execute(context);
            if (!conflictResolved) {
                throw new MultiMergeOperationException(
                        MessageBundle.message("error.push.source.unresolved", context.sourceBranch));
            }
            return;
        }

        if (result == StepResult.FAILURE) {
            throw new MultiMergeOperationException(
                    MessageBundle.message("error.push.source.failure", context.sourceBranch));
        }
    }
}