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
    private final boolean createRemoteIfMissing;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public PushBranchStep(GitRepositoryOperations service) {
        this.service = service;
        this.createRemoteIfMissing = false;
    }

    /**
     * Construtor com opção de criar a branch remota se não existir.
     *
     * @param service               Serviço de operações Git.
     * @param createRemoteIfMissing Se true, cria a branch remota se não existir.
     */
    public PushBranchStep(GitRepositoryOperations service, boolean createRemoteIfMissing) {
        this.service = service;
        this.createRemoteIfMissing = createRemoteIfMissing;
    }

    @Override
    public boolean execute(MergeContext context) {
        if (!context.pushAfterMerge) {
            return true;
        }

        // Verifica se a branch remota já existe
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.targetBranch);
        boolean needsSetupstream = remoteBranch == null;

        GitCommandResult pushResult = null;

        // Executa push somente se:
        // 1. A branch remota não existe e a opção de criar a branch remota está habilitada.
        // 2. Opção de criar a branch remota não está habilitada.
        if (!createRemoteIfMissing || needsSetupstream) {
            pushResult = service.push(context.repository, context.targetBranch, needsSetupstream);
        }

        notifyPushError(context, pushResult);
        return true;
    }

    /**
     * Notifica o usuário sobre erros no push.
     *
     * @param context    Contexto do merge.
     * @param pushResult Resultado do comando de push.
     */
    private void notifyPushError(MergeContext context, GitCommandResult pushResult) {
        if (pushResult != null && !pushResult.success()) {
            NotificationHelper.notifyWarning(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.push", context.targetBranch,
                            String.join("\n", pushResult.getErrorOutput())));
        }
    }
}