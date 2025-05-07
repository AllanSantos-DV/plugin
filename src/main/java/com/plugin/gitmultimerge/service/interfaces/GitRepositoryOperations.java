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
    /**
     * Faz checkout para a branch especificada.
     *
     * @param repository Repositório Git alvo.
     * @param branchName Nome da branch para checkout.
     * @return Resultado do comando Git.
     */
    GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String branchName);

    /**
     * Executa o merge da branch source na branch atual.
     *
     * @param repository    Repositório Git alvo.
     * @param sourceBranch  Nome da branch source.
     * @param squash        Se true, faz squash dos commits.
     * @param commitMessage Mensagem de commit para squash.
     * @return Resultado do comando Git.
     */
    GitCommandResult merge(@NotNull GitRepository repository, @NotNull String sourceBranch, boolean squash,
            String commitMessage);

    /**
     * Realiza push da branch especificada para o remote.
     *
     * @param repository Repositório Git alvo.
     * @param branchName Nome da branch para push.
     * @return Resultado do comando Git.
     */
    GitCommandResult push(@NotNull GitRepository repository, @NotNull String branchName);

    /**
     * Deleta a branch local especificada.
     *
     * @param repository Repositório Git alvo.
     * @param branchName Nome da branch a ser deletada.
     * @return Resultado do comando Git.
     */
    GitCommandResult deleteBranch(@NotNull GitRepository repository, @NotNull String branchName);

    /**
     * Busca a branch remota correspondente a uma branch local.
     *
     * @param repository      Repositório Git alvo.
     * @param localBranchName Nome da branch local.
     * @return Branch remota correspondente, ou null se não encontrada.
     */
    GitRemoteBranch findRemoteBranch(@NotNull GitRepository repository, @NotNull String localBranchName);

    /**
     * Deleta a branch remota especificada.
     *
     * @param repository   Repositório Git alvo.
     * @param remoteBranch Branch remota a ser deletada.
     * @return Resultado do comando Git.
     */
    GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository, @NotNull GitRemoteBranch remoteBranch);

    /**
     * Verifica se há alterações pendentes entre a branch atual e a branch de
     * origem.
     *
     * @param repository   Repositório Git alvo.
     * @param sourceBranch Nome da branch de origem.
     * @return true se há alterações pendentes, false caso contrário.
     */
    boolean hasPendingChanges(@NotNull GitRepository repository, @NotNull String sourceBranch);

    /**
     * Executa git fetch --all para atualizar referências remotas.
     *
     * @param repository Repositório Git alvo.
     */
    void fetchAll(@NotNull GitRepository repository);
}