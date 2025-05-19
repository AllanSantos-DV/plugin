package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que realiza o pull da branch target.
 */
public class PullBranchStep implements MergeStep {
    private final GitRepositoryOperations service;
    private String branchName;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public PullBranchStep(GitRepositoryOperations service) {
        this(service, null);
    }

    /**
     * Construtor com nome da branch.
     *
     * @param service    Serviço de operações Git.
     * @param branchName Nome da branch.
     */
    public PullBranchStep(GitRepositoryOperations service, String branchName) {
        this.service = service;
        this.branchName = branchName;
    }

    @Override
    public StepResult execute(MergeContext context) {
        if (branchName == null) {
            branchName = context.targetBranch;
        }
        GitCommandResult pullResult = service.pull(context.repository, branchName);
        if (pullResult.success()) {
            return StepResult.SUCCESS;
        }

        context.errorMessage = MessageBundle.message("error.pull", branchName,
                String.join("\n", pullResult.getErrorOutput()));

        return new ResultFailStep(pullResult, context.errorMessage).checkConflict(context);
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
