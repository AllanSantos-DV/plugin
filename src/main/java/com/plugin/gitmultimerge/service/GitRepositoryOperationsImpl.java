package com.plugin.gitmultimerge.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.util.MessageBundle;
import git4idea.GitRemoteBranch;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * @param setUpstream Se true, adiciona o parâmetro-u para criar e rastrear a
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

    /**
     * Realiza o pull da branch especificada.
     *
     * @param repository Repositório Git alvo.
     * @param branchName Nome da branch para pull.
     * @return Resultado do comando Git.
     */
    @Override
    public GitCommandResult pull(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PULL);
        handler.addParameters("origin", branchName);
        return git.runCommand(handler);
    }

    /** Delete a branch local especificada. */
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

    /** Delete a branch remota especificada. */
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

    @Override
    public GitCommandResult commit(@NotNull GitRepository repository, String commitMessage) {
        GitLineHandler commitHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.COMMIT);
        commitHandler.addParameters("--no-edit");
        if (commitMessage != null && !commitMessage.isEmpty()) {
            commitHandler.addParameters("-m", commitMessage);
        }
        return git.runCommand(commitHandler);
    }

    @Override
    public boolean isTargetUpToDateWithSource(@NotNull GitRepository repository, @NotNull String targetBranch,
            @NotNull String sourceBranch) {
        // Obtém o hash do merge-base entre target e source
        GitLineHandler mergeBaseHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE_BASE);
        mergeBaseHandler.addParameters(targetBranch, sourceBranch);
        GitCommandResult mergeBaseResult = git.runCommand(mergeBaseHandler);
        if (!mergeBaseResult.success() || mergeBaseResult.getOutput().isEmpty()) {
            return false;
        }
        String mergeBase = mergeBaseResult.getOutput().get(0).trim();

        // Obtém o hash do último commit da source
        GitLineHandler sourceHeadHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.REV_PARSE);
        sourceHeadHandler.addParameters(sourceBranch);
        GitCommandResult sourceHeadResult = git.runCommand(sourceHeadHandler);
        if (!sourceHeadResult.success() || sourceHeadResult.getOutput().isEmpty()) {
            return false;
        }
        String sourceHead = sourceHeadResult.getOutput().get(0).trim();

        // Se o merge-base é igual ao head da source, a target contém todos os commits
        // da source
        return mergeBase.equals(sourceHead);
    }

    @Override
    public Set<VirtualFile> getConflictedFiles(@NotNull GitRepository repository) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF);
        handler.addParameters("--name-only", "--diff-filter=U");
        GitCommandResult result = git.runCommand(handler);
        if (!result.success()) {
            return Collections.emptySet();
        }
        Set<VirtualFile> conflictedFiles = new HashSet<>();
        for (String path : result.getOutput()) {
            VirtualFile file = repository.getRoot().findFileByRelativePath(path);
            if (file != null) {
                conflictedFiles.add(file);
            }
        }
        return conflictedFiles;
    }

    @Override
    public void addFilesToIndex(@NotNull GitRepository repository, @NotNull List<VirtualFile> files) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.ADD);
        handler.addRelativeFiles(files);
        GitCommandResult result = git.runCommand(handler);
        if (!result.success()) {
            throw new RuntimeException(MessageBundle.message("error.git.add", result.getErrorOutputAsJoinedString()));
        }
    }

    @Override
    public void abortMerge(@NotNull GitRepository repository) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);
        handler.addParameters("--abort");
        git.runCommand(handler);
    }
}