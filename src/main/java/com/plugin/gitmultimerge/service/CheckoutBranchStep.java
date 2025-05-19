package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import git4idea.commands.GitCommandResult;

/**
 * Etapa de checkout para a branch target.
 */
public class CheckoutBranchStep implements MergeStep {
    private final GitRepositoryOperations service;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public CheckoutBranchStep(GitRepositoryOperations service) {
        this.service = service;
    }

    @Override
    public StepResult execute(MergeContext context) {
        if (context.atualBranch != null && context.atualBranch.equals(context.targetBranch)) {
            return StepResult.SUCCESS;
        }
        GitCommandResult checkoutResult = service.checkout(context.repository, context.targetBranch);
        if (checkoutResult.success()) {
            return StepResult.SUCCESS;
        }

        context.errorMessage = MessageBundle.message("error.checkout", context.targetBranch,
                String.join("\n", checkoutResult.getErrorOutput()));
        return new ResultFailStep(checkoutResult, context.errorMessage).checkConflict(context);
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