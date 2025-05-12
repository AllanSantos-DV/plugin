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
    private final boolean markedAsDeleted;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public PushBranchStep(GitRepositoryOperations service) {
        this.service = service;
        this.markedAsDeleted = false;
    }

    /**
     * Construtor com marcação de exclusão.
     *
     * @param service         Serviço de operações Git.
     * @param markedAsDeleted Indica se a branch foi marcada para exclusão.
     */
    public PushBranchStep(GitRepositoryOperations service, boolean markedAsDeleted) {
        this.service = service;
        this.markedAsDeleted = markedAsDeleted;
    }

    @Override
    public boolean execute(MergeContext context) {
        if (!context.pushAfterMerge || markedAsDeleted) {
            return true;
        }

        // Verifica se a branch remota já existe
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.targetBranch);
        boolean needsSetUpStream = remoteBranch == null;

        GitCommandResult pushResult = service.push(context.repository, context.targetBranch, needsSetUpStream);

        // Notifica o usuário sobre erros no push
        if (!pushResult.success()) {
            NotificationHelper.notifyWarning(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.push", context.targetBranch,
                            String.join("\n", pushResult.getErrorOutput())));
            // Não interrompe o fluxo, mas registra o erro
        }
        return true;
    }
}