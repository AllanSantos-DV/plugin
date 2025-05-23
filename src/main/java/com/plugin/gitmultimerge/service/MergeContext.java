package com.plugin.gitmultimerge.service;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import java.util.List;
import java.util.ArrayList;

/**
 * Contexto compartilhado entre as etapas do multi-merge.
 * Armazena parâmetros, progresso e resultados do fluxo.
 */
public class MergeContext {
    public final Project project;
    public final GitRepository repository;
    public final String sourceBranch;
    public final String targetBranch;
    public final boolean squash;
    public final boolean pushAfterMerge;
    public final boolean deleteSourceBranch;
    public final String commitMessage;
    public final ProgressIndicator indicator;

    // Resultados do merge
    public final List<String> successfulMerges = new ArrayList<>();
    public final List<String> failedMerges = new ArrayList<>();

    public boolean allSuccessful = true;
    public String errorMessage;
    public String atualBranch;

    /**
     * Construtor padrão.
     *
     * @param project            projeto
     * @param repository         repositório
     * @param sourceBranch       branch source
     * @param targetBranch       branch alvo
     * @param squash             se true, faz squash dos commits
     * @param pushAfterMerge     se true, faz push após o merge
     * @param deleteSourceBranch se true, delete a branch source após o merge
     * @param commitMessage      mensagem de commit para squash
     * @param indicator          indicador de progresso
     */
    public MergeContext(Project project, GitRepository repository, String sourceBranch, String targetBranch,
            boolean squash, boolean pushAfterMerge, boolean deleteSourceBranch, String commitMessage,
            ProgressIndicator indicator) {
        this.project = project;
        this.repository = repository;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.squash = squash;
        this.pushAfterMerge = pushAfterMerge;
        this.deleteSourceBranch = deleteSourceBranch;
        this.commitMessage = commitMessage;
        this.indicator = indicator;
    }
}