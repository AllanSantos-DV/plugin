package com.plugin.gitmultimerge.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.plugin.gitmultimerge.util.NotificationHelper;
import com.plugin.gitmultimerge.util.MessageBundle;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.commands.*;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementação do serviço que realiza operações Git para o Multi Merge.
 * Registrado como serviço no plugin.xml.
 */
@Service(Service.Level.PROJECT)
public final class GitMultiMergeServiceImpl implements GitMultiMergeService {

    private final Project project;
    private final Git git;

    public GitMultiMergeServiceImpl(Project project) {
        this.project = project;
        this.git = Git.getInstance();
    }

    @Override
    public List<String> getBranchNames(GitRepository repository) {
        List<String> branchNames = new ArrayList<>();
        Collection<GitLocalBranch> localBranches = repository.getBranches().getLocalBranches();

        for (GitLocalBranch branch : localBranches) {
            branchNames.add(branch.getName());
        }

        return branchNames;
    }

    @Override
    public CompletableFuture<Boolean> performMerge(
            GitRepository repository,
            String sourceBranch,
            List<String> targetBranches,
            boolean squash,
            boolean pushAfterMerge,
            boolean deleteSourceBranch,
            String commitMessage,
            ProgressIndicator indicator) {

        // Criar um CompletableFuture para retornar o resultado
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Executar a operação em background
        try {
            // Iniciar uma tarefa em background
            executeMultiMergeOperation(
                    repository,
                    sourceBranch,
                    targetBranches,
                    squash,
                    pushAfterMerge,
                    deleteSourceBranch,
                    commitMessage,
                    indicator,
                    future);
        } catch (Exception e) {
            NotificationHelper.notifyError(project, NotificationHelper.DEFAULT_TITLE, e);
            future.complete(false);
        }

        return future;
    }

    /**
     * Executa a operação de multi-merge, delegando cada etapa para métodos
     * auxiliares.
     */
    private void executeMultiMergeOperation(
            GitRepository repository,
            String sourceBranch,
            List<String> targetBranches,
            boolean squash,
            boolean pushAfterMerge,
            boolean deleteSourceBranch,
            String commitMessage,
            ProgressIndicator indicator,
            CompletableFuture<Boolean> future) {
        try {
            indicator.setIndeterminate(false);
            indicator.setText(MessageBundle.message("progress.preparing"));
            indicator.setFraction(0.0);

            String originalBranch = repository.getCurrentBranchName();
            if (originalBranch == null) {
                notifyErrorNoSource(future);
                return;
            }

            // Acumular resultados de todos os contextos
            List<String> allSuccessfulMerges = new ArrayList<>();
            List<String> allFailedMerges = new ArrayList<>();
            boolean allSuccessful = true;

            for (String targetBranch : targetBranches) {
                indicator.setText(MessageBundle.message("progress.processing", targetBranch));
                MergeContext context = new MergeContext(
                        project, repository, sourceBranch, targetBranch,
                        squash, pushAfterMerge, deleteSourceBranch, commitMessage, indicator);

                MergeStep[] steps = new MergeStep[] {
                        new ValidateUncommittedChangesStep(),
                        new CheckoutBranchStep(this),
                        new CheckUpToDateStep(this),
                        new PerformMergeStep(this),
                        new PushBranchStep(this)
                };
                for (MergeStep step : steps) {
                    if (!step.execute(context)) {
                        break;
                    }
                }
                allSuccessfulMerges.addAll(context.successfulMerges);
                allFailedMerges.addAll(context.failedMerges);
                if (!context.allSuccessful) {
                    allSuccessful = false;
                }
            }

            indicator.setText(MessageBundle.message("progress.returning"));
            indicator.setFraction(1.0);
            new ReturnToOriginalBranchStep(this, originalBranch).execute(
                    new MergeContext(project, repository, sourceBranch, originalBranch, squash, pushAfterMerge,
                            deleteSourceBranch, commitMessage, indicator));
            new DeleteSourceBranchStep(this, originalBranch).execute(
                    new MergeContext(project, repository, sourceBranch, originalBranch, squash, pushAfterMerge,
                            deleteSourceBranch, commitMessage, indicator));

            StringBuilder summary = new StringBuilder();
            if (!allSuccessfulMerges.isEmpty()) {
                summary.append(MessageBundle.message("summary.successful.merges",
                        String.join(", ", allSuccessfulMerges)));
            }
            if (!allFailedMerges.isEmpty()) {
                if (!summary.isEmpty())
                    summary.append("\n\n");
                summary.append(MessageBundle.message("summary.failed.merges",
                        String.join(", ", allFailedMerges)));
            }
            if (allSuccessful) {
                NotificationHelper.notifySuccess(
                        project,
                        NotificationHelper.DEFAULT_TITLE,
                        summary.toString());
            } else {
                NotificationHelper.notifyWarning(
                        project,
                        NotificationHelper.DEFAULT_TITLE,
                        summary.toString());
            }
            future.complete(allSuccessful);
        } catch (Exception e) {
            NotificationHelper.notifyError(project, NotificationHelper.DEFAULT_TITLE, e);
            future.complete(false);
        }
    }

    /** Notifica erro e completa o future caso a branch source seja inválida. */
    private void notifyErrorNoSource(CompletableFuture<Boolean> future) {
        NotificationHelper.notifyError(
                project,
                NotificationHelper.DEFAULT_TITLE,
                MessageBundle.message("error.no.source"));
        future.complete(false);
    }

    /**
     * Verifica se há alterações não commitadas no working directory
     * 
     * @return true se existem alterações não commitadas, false se o working
     *         directory está limpo
     */
    @Override
    public boolean hasUncommittedChanges(@NotNull GitRepository repository) {
        GitLineHandler statusHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.STATUS);
        statusHandler.addParameters("--porcelain");
        GitCommandResult statusResult = git.runCommand(statusHandler);

        // Se a saída não estiver vazia, significa que há alterações não commitadas
        return statusResult.getOutput().stream().anyMatch(line -> !line.trim().isEmpty());
    }

    /**
     * Verifica se há alterações pendentes entre a branch atual e a branch de origem
     * 
     * @return true se existem alterações, false se as branches estão idênticas
     */
    public boolean hasPendingChanges(@NotNull GitRepository repository, @NotNull String sourceBranch) {
        GitLineHandler diffHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF);
        diffHandler.addParameters(sourceBranch, "--name-only");
        GitCommandResult diffResult = git.runCommand(diffHandler);

        // Retorna true se houver pelo menos uma linha não vazia na saída do diff
        return diffResult.getOutput().stream().anyMatch(line -> !line.trim().isEmpty());
    }

    /**
     * Faz o checkout para uma branch específica
     */
    public GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
        handler.addParameters(branchName);
        return git.runCommand(handler);
    }

    /**
     * Executa o merge com a branch source
     */
    public GitCommandResult merge(@NotNull GitRepository repository, @NotNull String sourceBranch,
            boolean squash, String commitMessage) {
        // Processo normal de merge
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);

        if (squash) {
            handler.addParameters("--squash");
        }

        handler.addParameters(sourceBranch);

        GitCommandResult result = git.runCommand(handler);

        // Se foi com squash e deu sucesso, precisamos fazer o commit
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
     * Faz push da branch atual
     */
    public GitCommandResult push(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
        handler.addParameters("origin", branchName);
        return git.runCommand(handler);
    }

    /**
     * Deleta uma branch local
     */
    public GitCommandResult deleteBranch(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.BRANCH);
        handler.addParameters("-D", branchName);
        return git.runCommand(handler);
    }

    /**
     * Encontra a branch remota correspondente a uma branch local
     */
    public GitRemoteBranch findRemoteBranch(@NotNull GitRepository repository, @NotNull String localBranchName) {
        for (GitRemoteBranch remoteBranch : repository.getBranches().getRemoteBranches()) {
            if (remoteBranch.getNameForLocalOperations().equals("origin/" + localBranchName)) {
                return remoteBranch;
            }
        }
        return null;
    }

    /**
     * Deleta uma branch remota
     */
    GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository,
            @NotNull GitRemoteBranch remoteBranch) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
        handler.addParameters("origin", "--delete", remoteBranch.getNameForRemoteOperations());
        return git.runCommand(handler);
    }
}