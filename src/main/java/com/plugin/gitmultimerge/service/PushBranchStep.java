package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que realiza o push da branch target, se necessário.
 */
public class PushBranchStep implements MergeStep {
    private final GitRepositoryOperations service;

    public PushBranchStep(GitRepositoryOperations service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        if (!context.pushAfterMerge) {
            return true;
        }
        // Verifica se a branch remota já existe
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.targetBranch);

        // Se a branch remota já existe, não precisa fazer push
        boolean setupstream = remoteBranch == null;

        // Se a branch remota não existe, faz push com -u
        GitCommandResult pushResult = service.push(context.repository, context.targetBranch, setupstream);
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