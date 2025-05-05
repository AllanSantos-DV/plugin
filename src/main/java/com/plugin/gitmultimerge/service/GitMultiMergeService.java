package com.plugin.gitmultimerge.service;

import com.intellij.openapi.progress.ProgressIndicator;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface de serviço que define as operações Git necessárias para o Multi
 * Merge.
 * Separa a lógica de negócio da interface do usuário.
 */
public interface GitMultiMergeService {

    /**
     * Obtém a lista de nomes de branches no repositório.
     *
     * @param repository O repositório Git
     * @return Lista de nomes de branches
     */
    List<String> getBranchNames(GitRepository repository);

    /**
     * Realiza a operação de merge da branch source para múltiplas branches target.
     *
     * @param repository         O repositório Git
     * @param sourceBranch       Nome da branch source
     * @param targetBranches     Lista de branches target
     * @param squash             Se deve aplicar squash aos commits
     * @param pushAfterMerge     Se deve fazer push após o merge
     * @param deleteSourceBranch Se deve deletar a branch source após o merge
     * @param commitMessage      Mensagem de commit opcional para squash
     * @param indicator          Indicador de progresso para feedback visual
     * @return Future que completa com true se todas as operações forem
     *         bem-sucedidas
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
     * Verifica se há alterações não commitadas no working directory
     *
     * @return true se existem alterações não commitadas, false se o working
     *         directory está limpo
     */
    boolean hasUncommittedChanges(@NotNull GitRepository repository);
}