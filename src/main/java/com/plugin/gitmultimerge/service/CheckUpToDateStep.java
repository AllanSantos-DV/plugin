package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;

/**
 * Etapa que verifica se a branch target já está atualizada em relação à source.
 */
public class CheckUpToDateStep implements MergeStep {
    private final GitRepositoryOperations service;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public CheckUpToDateStep(GitRepositoryOperations service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        assert context.sourceBranch != null;
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