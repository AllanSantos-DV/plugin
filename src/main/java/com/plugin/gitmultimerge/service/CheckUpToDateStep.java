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
    public StepResult execute(MergeContext context) {
        boolean branchAlreadyUpToDate = service.isTargetUpToDateWithSource(
                context.repository, context.targetBranch, context.sourceBranch);
        if (branchAlreadyUpToDate) {
            NotificationHelper.notifyInfo(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("notification.already.up.to.date", context.targetBranch,
                            context.sourceBranch));
            context.successfulMerges.add(context.targetBranch);
            return StepResult.SKIPPED; // Interrompe o fluxo para esta branch, mas não é erro crítico.
        }
        return StepResult.SUCCESS;
    }

    @Override
    public StepResult failure(MergeContext context) {
        // Não há falha nesta etapa, apenas retorna sucesso.
        return StepResult.SKIPPED;
    }

    @Override
    public void success(MergeContext context) {
        // Não há ações específicas a serem realizadas em caso de sucesso nesta etapa.
    }
}