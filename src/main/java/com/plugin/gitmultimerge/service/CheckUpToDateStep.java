package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;

/**
 * Etapa que verifica se a branch target já está atualizada em relação à source.
 */
public class CheckUpToDateStep implements MergeStep {
    private final GitMultiMergeServiceImpl service;

    public CheckUpToDateStep(GitMultiMergeServiceImpl service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        boolean branchAlreadyUpToDate = !service.hasPendingChanges(context.repository, context.sourceBranch);
        if (branchAlreadyUpToDate) {
            NotificationHelper.notifyInfo(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("notification.already.up.to.date", context.targetBranch,
                            context.sourceBranch));
            context.successfulMerges.add(context.targetBranch);
            return false; // Interrompe o fluxo para esta branch
        }
        return true;
    }
}