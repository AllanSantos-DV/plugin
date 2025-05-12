package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

/**
 * Etapa que realiza o pull da branch target.
 */
public class PullBranchStep implements MergeStep {
    private final GitRepositoryOperations service;

    /**
     * Construtor padrão.
     *
     * @param service Serviço de operações Git.
     */
    public PullBranchStep(GitRepositoryOperations service) {
        this.service = service;
    }

    @Override
    public boolean execute(MergeContext context) {
        // Executa o pull da branch target
        GitCommandResult pullResult = service.pull(context.repository, context.targetBranch);

        // Notifica erro caso o pull falhe
        if (!pullResult.success()) {
            NotificationHelper.notifyError(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    MessageBundle.message("error.pull", context.targetBranch,
                            String.join("\n", pullResult.getErrorOutput())));
            // Não interrompe o fluxo, mas registra o erro
        }
        return true;
    }
}
