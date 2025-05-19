package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.sun.istack.Nullable;
import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que realiza o push da branch target, se necessário.
 */
public class PushBranchStep implements MergeStep {
    private final GitRepositoryOperations service;
    private final boolean markedAsDeleted;
    private String branchName;
    @Nullable
    private Boolean remoteNotExists;

    /**
     * Construtor com nome da branch e verificação de remote.
     * @param service Serviço de operações Git.
     * @param remoteNotExists ‘Flag’ a indicar se a branch remota não exista.
     */
    public PushBranchStep(GitRepositoryOperations service, boolean remoteNotExists) {
        this(service, null, false);
        this.remoteNotExists = remoteNotExists;
    }

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     * @param branchName Nome da branch.
     * @param markedAsDeleted ‘Flag’ a indicar se a branch foi marcada para exclusão.
     */
    public PushBranchStep(GitRepositoryOperations service, String branchName, boolean markedAsDeleted) {
        this.service = service;
        this.branchName = branchName;
        this.markedAsDeleted = markedAsDeleted;
        this.remoteNotExists = null;
    }

    @Override
    public StepResult execute(MergeContext context) {
        if (!context.pushAfterMerge || markedAsDeleted) {
            return StepResult.SUCCESS;
        }

        if (branchName == null) {
            branchName = context.targetBranch;
        }

        if (remoteNotExists == null) {
            GitRemoteBranch remoteBranch = service.findRemoteBranch(context.repository, branchName);
            remoteNotExists = remoteBranch == null;
        }
        GitCommandResult pushResult = service.push(context.repository, branchName, remoteNotExists);
        if (pushResult.success()) {
            return StepResult.SUCCESS;
        }

        context.errorMessage = MessageBundle.message("error.push", branchName,
                String.join("\n", pushResult.getErrorOutput()));

        return new ResultFailStep(pushResult, context.errorMessage).checkConflict(context);
    }

    @Override
    public StepResult failure(MergeContext context) {
        context.allSuccessful = false;
        context.failedMerges.add(context.targetBranch);
        return StepResult.SKIPPED;
    }

    @Override
    public void success(MergeContext context) {
        // Não há ações específicas a serem realizadas em caso de sucesso nesta etapa.
    }
}