package com.plugin.gitmultimerge.service.interfaces;

import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Operações Git de baixo nível sobre o repositório.
 * Permite executar comandos essenciais para o fluxo de merge.
 */
public interface GitRepositoryOperations {
    /** Faz checkout para a branch especificada. */
    GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String branchName);

    /** Executa o merge da branch source na branch atual. */
    GitCommandResult merge(@NotNull GitRepository repository, @NotNull String sourceBranch, boolean squash,
            String commitMessage);

    /** Realiza push da branch especificada para o remote. */
    GitCommandResult push(@NotNull GitRepository repository, @NotNull String branchName);

    /** Deleta a branch local especificada. */
    GitCommandResult deleteBranch(@NotNull GitRepository repository, @NotNull String branchName);

    /** Busca a branch remota correspondente a uma branch local. */
    GitRemoteBranch findRemoteBranch(@NotNull GitRepository repository, @NotNull String localBranchName);

    /** Deleta a branch remota especificada. */
    GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository, @NotNull GitRemoteBranch remoteBranch);

    /**
     * Verifica se há alterações pendentes entre a branch atual e a branch de
     * origem.
     */
    boolean hasPendingChanges(@NotNull GitRepository repository, @NotNull String sourceBranch);

    /**
     * Executa git fetch --all para atualizar referências remotas.
     */
    void fetchAll(@NotNull GitRepository repository);
}