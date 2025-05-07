package com.plugin.gitmultimerge.service;

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
    /** Lista os nomes das branches locais do repositório. */
    List<String> getBranchNames(GitRepository repository);

    /**
     * Executa o merge da branch source para múltiplas branches target.
     * Opera de forma assíncrona e notifica o progresso.
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

    /** Verifica se há alterações não commitadas no working directory. */
    boolean hasUncommittedChanges(@NotNull GitRepository repository);
}