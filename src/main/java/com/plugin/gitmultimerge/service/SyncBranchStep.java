package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import git4idea.GitRemoteBranch;

public class SyncBranchStep implements MergeStep {
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
    public StepResult execute(MergeContext context) {
        // Verifica se a branch remota já existe.
        GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, context.targetBranch);
        boolean needsSetUpStream = remoteBranch == null;

        if (context.pushAfterMerge && needsSetUpStream){
            return new PushBranchStep(service, true).execute(context);
        }
        return new PullBranchStep(service).execute(context);
    }

    @Override
    public StepResult failure(MergeContext context) {
        context.allSuccessful = false;
        return StepResult.SKIPPED;
    }

    @Override
    public void success(MergeContext context) {
        // Não há ações específicas a serem realizadas em caso de sucesso nesta etapa.
    }
}
