package com.plugin.gitmultimerge.service.interfaces;

import com.intellij.openapi.progress.ProgressIndicator;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço principal para operações de multi-merge Git.
 * Orquestra o fluxo de merge entre branches.
 */
public interface GitMultiMergeService {
    /**
     * Lista os nomes das branches locais do repositório.
     *
     * @param repository Repositório Git alvo.
     * @return Lista de nomes das branches locais.
     */
    List<String> getBranchNames(GitRepository repository);

    /**
     * Executa o merge da branch source para múltiplas branches target.
     * Opera de forma assíncrona e notifica o progresso.
     *
     * @param repository         Repositório Git alvo.
     * @param sourceBranch       Nome da branch source.
     * @param targetBranches     Lista de branches target.
     * @param squash             Se true, faz squash dos commits.
     * @param pushAfterMerge     Se true, faz push após cada merge.
     * @param deleteSourceBranch Se true, remove a branch source após merges
     *                           bem-sucedidos.
     * @param commitMessage      Mensagem de commit para squash.
     * @param indicator          Indicador de progresso.
     * @return CompletableFuture indicando sucesso ou falha da operação.
     */
    CompletableFuture<Boolean> performMerge(
            GitRepository repository,
            String sourceBranch,
            List<String> targetBranches,
            boolean squash,
            boolean pushAfterMerge,
            boolean deleteSourceBranch,
            String commitMessage,
            ProgressIndicator indicator);

    /**
     * Verifica se há alterações não enviadas no working directory.
     *
     * @param repository Repositório Git alvo.
     * @return true se existem alterações não enviadas, false se o working
     *         directory está limpo.
     */
    boolean hasUncommittedChanges(@NotNull GitRepository repository);
}