package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;

/**
 * Etapa de validação de alterações não commitadas antes do merge.
 */
public class ValidateUncommittedChangesStep implements MergeStep {
    @Override
    public boolean execute(MergeContext context) {
        boolean hasUncommittedChanges = context.repository != null && context.project != null && context.repository.getProject().equals(context.project) && context.repository.getCurrentBranchName() != null && context.repository.getCurrentBranchName().equals(context.targetBranch) && context.repository.getBranches().getLocalBranches().stream()
                .anyMatch(branch -> branch.getName().equals(context.targetBranch));

        // Se houver alterações não commitadas, notifica e interrompe o fluxo
        if (hasUncommittedChanges) {
            if (context.squash) {
                NotificationHelper.notifyError(
                        context.project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("error.uncommitted.changes.squash", context.targetBranch));
            } else {
                NotificationHelper.notifyError(
                        context.project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("error.uncommitted.changes.details", context.targetBranch));
            }
            context.allSuccessful = false;
            context.failedMerges.add(context.targetBranch);
            return false;
        }
        return true;
    }
}