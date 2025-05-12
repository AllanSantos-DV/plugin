package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import git4idea.GitRemoteBranch;

public class SyncBranchStep implements MergeStep{
    private final GitRepositoryOperations service;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    protected SyncBranchStep(GitRepositoryOperations service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        // Verifica se o push após o merge está habilitado
        if (!context.pushAfterMerge) {
            return true;
        }
        // Verifica se a branch remota já existe
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.targetBranch);
        boolean needsSetUpStream = remoteBranch == null;

        // Se a branch remota não existe, executa o push com setUpStream
        // Se a branch remota existe, executa o pull para garantir que a branch local esteja atualizada
        if (needsSetUpStream) return new PushBranchStep(service).execute(context);
        return new PullBranchStep(service).execute(context);
    }
}
