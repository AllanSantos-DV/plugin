package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.util.MessageBundle;
import git4idea.commands.GitCommandResult;

public class AutoCommitStep {
    private final GitRepositoryOperations service;

    public AutoCommitStep(GitRepositoryOperations service) {
        this.service = service;
    }

    public boolean autoCommit(MergeContext context) {
        String conflictCommitMessage = context.commitMessage != null
                && !context.commitMessage.isEmpty()
                ? context.commitMessage
                : MessageBundle.message("commit.conflict.resolution");
        GitCommandResult commitResult = service.commit(context.repository, conflictCommitMessage);
        if (commitResult.success()) {
            StepResult pushResult = new PushBranchStep(service, false).execute(context);
            if (pushResult == StepResult.SUCCESS) {
                return true;
            }
        }
        context.errorMessage = MessageBundle.message("error.commit", context.targetBranch,
                String.join("\n", commitResult.getErrorOutput()));
        return false;
    }
}
