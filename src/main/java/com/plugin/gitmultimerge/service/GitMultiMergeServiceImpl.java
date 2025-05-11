package com.plugin.gitmultimerge.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.plugin.gitmultimerge.service.interfaces.GitMultiMergeService;
import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import com.plugin.gitmultimerge.service.interfaces.MergeStep;
import com.plugin.gitmultimerge.util.NotificationHelper;
import com.plugin.gitmultimerge.util.MessageBundle;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;

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
    private final GitRepositoryOperations gitOps;

    public GitMultiMergeServiceImpl(Project project) {
        this.project = project;
        this.gitOps = new GitRepositoryOperationsImpl(project);
    }

    /**
     * Lista os nomes das branches locais do repositório.
     *
     * @param repository Repositório Git alvo.
     * @return Lista de nomes das branches locais.
     */
    @Override
    public List<String> getBranchNames(GitRepository repository) {
        List<String> branchNames = new ArrayList<>();
        Collection<GitLocalBranch> localBranches = repository.getBranches().getLocalBranches();

        for (GitLocalBranch branch : localBranches) {
            branchNames.add(branch.getName());
        }

        return branchNames;
    }

    /**
     * Executa o merge da branch source para múltiplas branches target.
     * Opera de forma assíncrona e notifica o progresso.
     *
     * @param repository         Repositório Git alvo.
     * @param sourceBranch       Nome da branch source.
     * @param targetBranches     Lista de branches target.
     * @param squash             Se true, faz squash dos commits.
     * @param pushAfterMerge     Se true, faz push após cada merge.
     * @param deleteSourceBranch Se true, deleta a branch source após merges
     *                           bem-sucedidos.
     * @param commitMessage      Mensagem de commit para squash.
     * @param indicator          Indicador de progresso.
     * @return CompletableFuture indicando sucesso ou falha da operação.
     */
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

            MergeResult result = processTargetBranches(
                    repository, sourceBranch, targetBranches, squash, pushAfterMerge, deleteSourceBranch, commitMessage,
                    indicator);

            boolean shouldDelete = deleteSourceBranch && result.allSuccessful;
            boolean shouldReturnToOriginal = !shouldDelete || !originalBranch.equals(sourceBranch);

            indicator.setText(MessageBundle.message("progress.returning"));
            indicator.setFraction(1.0);

            if (shouldReturnToOriginal) {
                handleReturnToOriginalBranch(repository, sourceBranch, originalBranch, squash, pushAfterMerge,
                        deleteSourceBranch, commitMessage, indicator);
            }

            if (shouldDelete) {
                deleteSourceBranch = handleDeleteSourceBranch(repository, sourceBranch, originalBranch,
                        targetBranches.get(0), squash,
                        pushAfterMerge, commitMessage, indicator);
            } else if (deleteSourceBranch) {
                NotificationHelper.notifyWarning(
                        project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("summary.delete.skipped"));
            }

            handleFetchIfNeeded(repository, pushAfterMerge, deleteSourceBranch);
            notifySummary(result.allSuccessfulMerges, result.allFailedMerges, result.allSuccessful);
            future.complete(result.allSuccessful);
        } catch (Exception e) {
            NotificationHelper.notifyError(project, NotificationHelper.DEFAULT_TITLE, e);
            future.complete(false);
        }
    }

    /** Estrutura para acumular resultados do processamento das targets. */
    private static class MergeResult {
        final List<String> allSuccessfulMerges = new ArrayList<>();
        final List<String> allFailedMerges = new ArrayList<>();
        boolean allSuccessful = true;
    }

    /** Processa o merge para todas as branches target. */
    private MergeResult processTargetBranches(
            GitRepository repository,
            String sourceBranch,
            List<String> targetBranches,
            boolean squash,
            boolean pushAfterMerge,
            boolean deleteSourceBranch,
            String commitMessage,
            ProgressIndicator indicator) {
        MergeResult result = new MergeResult();
        for (String targetBranch : targetBranches) {
            indicator.setText(MessageBundle.message("progress.processing", targetBranch));
            MergeContext context = new MergeContext(
                    project, repository, sourceBranch, targetBranch,
                    squash, pushAfterMerge, deleteSourceBranch, commitMessage, indicator);
            MergeStep[] steps = new MergeStep[] {
                    new CheckoutBranchStep(gitOps),
                    new PushBranchStep(gitOps, true),
                    new PullBranchStep(gitOps),
                    new CheckUpToDateStep(gitOps),
                    new PerformMergeStep(gitOps),
                    new PushBranchStep(gitOps)
            };
            for (MergeStep step : steps) {
                if (!step.execute(context)) {
                    break;
                }
            }
            result.allSuccessfulMerges.addAll(context.successfulMerges);
            result.allFailedMerges.addAll(context.failedMerges);
            if (!context.allSuccessful) {
                result.allSuccessful = false;
            }
        }
        return result;
    }

    /** Retorna para a branch original, se necessário. */
    private void handleReturnToOriginalBranch(
            GitRepository repository,
            String sourceBranch,
            String originalBranch,
            boolean squash,
            boolean pushAfterMerge,
            boolean deleteSourceBranch,
            String commitMessage,
            ProgressIndicator indicator) {
        new ReturnToOriginalBranchStep(gitOps, originalBranch).execute(
                new MergeContext(project, repository, sourceBranch, originalBranch, squash, pushAfterMerge,
                        deleteSourceBranch, commitMessage, indicator));
    }

    /** Deleta a branch source, se necessário. */
    private boolean handleDeleteSourceBranch(
            GitRepository repository,
            String sourceBranch,
            String originalBranch,
            String targetBranch,
            boolean squash,
            boolean pushAfterMerge,
            String commitMessage,
            ProgressIndicator indicator) {
        return new DeleteSourceBranchStep(gitOps, originalBranch).execute(
                new MergeContext(project, repository, sourceBranch, targetBranch, squash, pushAfterMerge,
                        true, commitMessage, indicator));
    }

    /** Executa fetch --all se houve push ou deleção. */
    private void handleFetchIfNeeded(GitRepository repository, boolean pushAfterMerge, boolean anyDelete) {
        if (pushAfterMerge || anyDelete) {
            gitOps.fetchAll(repository, anyDelete);
        }
    }

    /** Notifica o resumo das operações ao usuário. */
    private void notifySummary(List<String> allSuccessfulMerges, List<String> allFailedMerges, boolean allSuccessful) {
        StringBuilder summary = new StringBuilder();
        if (!allSuccessfulMerges.isEmpty()) {
            summary.append(MessageBundle.message("summary.successful.merges",
                    String.join(", ", allSuccessfulMerges)));
        }
        if (!allFailedMerges.isEmpty()) {
            if (!summary.isEmpty())
                summary.append("<br>");
            summary.append(MessageBundle.message("summary.failed.merges",
                    String.join(", ", allFailedMerges)));
        }
        String htmlSummary = "<html>" + summary + "</html>";
        if (allSuccessful) {
            NotificationHelper.notifySuccess(
                    project,
                    NotificationHelper.DEFAULT_TITLE,
                    htmlSummary);
        } else {
            NotificationHelper.notifyWarning(
                    project,
                    NotificationHelper.DEFAULT_TITLE,
                    htmlSummary);
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
     * Verifica se há alterações não commitadas no working directory.
     *
     * @param repository Repositório Git alvo.
     * @return true se existem alterações não commitadas, false se o working
     *         directory está limpo.
     */
    @Override
    public boolean hasUncommittedChanges(@NotNull GitRepository repository) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        VirtualFile root = repository.getRoot();
        // Verifica se há arquivos modificados, staged ou não, no repositório
        return changeListManager.getAffectedFiles().stream()
                .anyMatch(file -> file.getPath().startsWith(root.getPath()));
    }
}