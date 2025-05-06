package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

/**
 * Etapa de checkout para a branch target.
 */
public class CheckoutBranchStep implements MergeStep {
    private final GitMultiMergeServiceImpl service;

    public CheckoutBranchStep(GitMultiMergeServiceImpl service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        GitCommandResult checkoutResult = service.checkout(context.repository, context.targetBranch);
        if (!checkoutResult.success()) {
            NotificationHelper.notifyError(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.checkout", context.targetBranch,
                            String.join("\n", checkoutResult.getErrorOutput())));
            context.allSuccessful = false;
            context.failedMerges.add(context.targetBranch);
            return false;
        }
        return true;
    }
}