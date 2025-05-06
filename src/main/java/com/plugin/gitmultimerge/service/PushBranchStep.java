package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que realiza o push da branch target, se necessário.
 */
public class PushBranchStep implements MergeStep {
    private final GitMultiMergeServiceImpl service;

    public PushBranchStep(GitMultiMergeServiceImpl service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        if (!context.pushAfterMerge) {
            return true; // Não precisa executar push
        }
        GitCommandResult pushResult = service.push(context.repository, context.targetBranch);
        if (!pushResult.success()) {
            NotificationHelper.notifyWarning(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.push", context.targetBranch,
                            String.join("\n", pushResult.getErrorOutput())));
            // Não interrompe o fluxo, apenas avisa
        }
        return true;
    }
}