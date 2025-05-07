package com.plugin.gitmultimerge.service;

import com.intellij.openapi.project.Project;
import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import git4idea.GitRemoteBranch;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

/**
 * Implementação padrão das operações Git de baixo nível.
 */
public class GitRepositoryOperationsImpl implements GitRepositoryOperations {
    private final Project project;
    private final Git git;

    public GitRepositoryOperationsImpl(Project project) {
        this.project = project;
        this.git = Git.getInstance();
    }

    /** Faz checkout para a branch especificada. */
    @Override
    public GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
        handler.addParameters(branchName);
        return git.runCommand(handler);
    }

    /** Executa o merge da branch source na branch atual. */
    @Override
    public GitCommandResult merge(@NotNull GitRepository repository, @NotNull String sourceBranch, boolean squash,
            String commitMessage) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);
        if (squash) {
            handler.addParameters("--squash");
        }
        handler.addParameters(sourceBranch);
        GitCommandResult result = git.runCommand(handler);
        if (squash && result.success()) {
            GitLineHandler commitHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.COMMIT);
            commitHandler.addParameters("--no-edit");
            if (commitMessage != null && !commitMessage.isEmpty()) {
                commitHandler.addParameters("-m", commitMessage);
            }
            return git.runCommand(commitHandler);
        }
        return result;
    }

    /**
     * Realiza push da branch especificada para o remote, com opção de setUpstream.
     *
     * @param repository  Repositório Git alvo.
     * @param branchName  Nome da branch para push.
     * @param setUpstream Se true, adiciona o parâmetro -u para criar e rastrear a
     *                    branch remota.
     * @return Resultado do comando Git.
     */
    @Override
    public GitCommandResult push(@NotNull GitRepository repository, @NotNull String branchName, boolean setUpstream) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
        if (setUpstream) {
            handler.addParameters("-u");
        }
        handler.addParameters("origin", branchName);
        return git.runCommand(handler);
    }

    /** Deleta a branch local especificada. */
    @Override
    public GitCommandResult deleteBranch(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.BRANCH);
        handler.addParameters("-D", branchName);
        return git.runCommand(handler);
    }

    /** Busca a branch remota correspondente a uma branch local. */
    @Override
    public GitRemoteBranch findRemoteBranch(@NotNull GitRepository repository, @NotNull String localBranchName) {
        for (GitRemoteBranch remoteBranch : repository.getBranches().getRemoteBranches()) {
            if (remoteBranch.getNameForLocalOperations().equals("origin/" + localBranchName)) {
                return remoteBranch;
            }
        }
        return null;
    }

    /** Deleta a branch remota especificada. */
    @Override
    public GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository,
            @NotNull GitRemoteBranch remoteBranch) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
        handler.addParameters("origin", "--delete", remoteBranch.getNameForRemoteOperations());
        return git.runCommand(handler);
    }

    /**
     * Verifica se há alterações pendentes entre a branch atual e a branch de
     * origem.
     */
    @Override
    public boolean hasPendingChanges(@NotNull GitRepository repository, @NotNull String sourceBranch) {
        GitLineHandler diffHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF);
        diffHandler.addParameters(sourceBranch, "--name-only");
        GitCommandResult diffResult = git.runCommand(diffHandler);

        // Retorna true se houver pelo menos uma linha não vazia na saída do diff
        return diffResult.getOutput().stream().anyMatch(line -> !line.trim().isEmpty());
    }

    /**
     * Executa git fetch --all para atualizar referências remotas.
     */
    @Override
    public void fetchAll(@NotNull GitRepository repository, boolean deletedBranches) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.FETCH);
        handler.addParameters("--all");
        if (deletedBranches) {
            handler.addParameters("--prune");
        }
        git.runCommand(handler);
        // Atualiza o repositório na IDE após o fetch
        GitRepositoryManager.getInstance(project).updateRepository(repository.getRoot());
    }
}