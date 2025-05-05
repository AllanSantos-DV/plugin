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
     * Executa a operação de multi-merge.
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
            // Inicializa o indicador de progresso
            indicator.setIndeterminate(false);
            indicator.setText(MessageBundle.message("progress.preparing"));
            indicator.setFraction(0.0);

            // Salva a branch atual para retornar a ela depois
            String originalBranch = repository.getCurrentBranchName();
            if (originalBranch == null) {
                NotificationHelper.notifyError(
                        project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("error.no.source"));
                future.complete(false);
                return;
            }

            // Calcula o progresso total: 1 etapa por branch + voltar para branch original
            double totalSteps = targetBranches.size() + 1;
            double currentStep = 0;

            boolean allSuccessful = true;
            List<String> successfulMerges = new ArrayList<>();
            List<String> failedMerges = new ArrayList<>();

            // Para cada branch target
            for (String targetBranch : targetBranches) {
                indicator.setText(MessageBundle.message("progress.processing", targetBranch));
                indicator.setFraction(currentStep / totalSteps);

                // 0. Verifica se há alterações não commitadas no working directory
                boolean hasUncommittedChanges = hasUncommittedChanges(repository);
                if (hasUncommittedChanges) {
                    // Mostra uma mensagem de erro específica para squash, se aplicável
                    if (squash) {
                        NotificationHelper.notifyError(
                                project,
                                NotificationHelper.DEFAULT_TITLE,
                                MessageBundle.message("error.uncommitted.changes.squash", targetBranch));
                    } else {
                        NotificationHelper.notifyError(
                                project,
                                NotificationHelper.DEFAULT_TITLE,
                                MessageBundle.message("error.uncommitted.changes.details", targetBranch));
                    }

                    allSuccessful = false;
                    failedMerges.add(targetBranch);
                    // Não continua para o checkout/merge desta branch
                    currentStep++;
                    continue;
                }

                // 1. Checkout na branch target
                GitCommandResult checkoutResult = checkout(repository, targetBranch);
                if (!checkoutResult.success()) {
                    NotificationHelper.notifyError(
                            project,
                            NotificationHelper.DEFAULT_TITLE,
                            MessageBundle.message("error.checkout", targetBranch,
                                    String.join("\n", checkoutResult.getErrorOutput())));
                    allSuccessful = false;
                    failedMerges.add(targetBranch);
                    continue;
                }

                // 2. Verifica primeiro se há alterações para mesclar
                boolean branchAlreadyUpToDate = !hasPendingChanges(repository, sourceBranch);

                if (branchAlreadyUpToDate) {
                    // Se não há alterações, mostra notificação informativa e continua
                    NotificationHelper.notifyInfo(
                            project,
                            NotificationHelper.DEFAULT_TITLE,
                            MessageBundle.message("notification.already.up.to.date", targetBranch, sourceBranch));

                    // Marca como sucesso e continua para a próxima branch
                    successfulMerges.add(targetBranch);
                    currentStep++;
                    continue;
                }

                // 3. Executa o merge apenas se houver alterações
                GitCommandResult mergeResult = merge(repository, sourceBranch, squash, commitMessage);
                if (!mergeResult.success()) {
                    NotificationHelper.notifyError(
                            project,
                            NotificationHelper.DEFAULT_TITLE,
                            MessageBundle.message("error.merge", sourceBranch, targetBranch,
                                    String.join("\n", mergeResult.getErrorOutput())));
                    allSuccessful = false;
                    failedMerges.add(targetBranch);
                    continue;
                }

                successfulMerges.add(targetBranch);

                // 4. Push se necessário (apenas se houve merge)
                if (pushAfterMerge) {
                    indicator.setText(MessageBundle.message("progress.pushing", targetBranch));
                    GitCommandResult pushResult = push(repository, targetBranch);
                    if (!pushResult.success()) {
                        NotificationHelper.notifyWarning(
                                project,
                                NotificationHelper.DEFAULT_TITLE,
                                MessageBundle.message("error.push", targetBranch,
                                        String.join("\n", pushResult.getErrorOutput())));
                    }
                }

                currentStep++;
            }

            // Volta para a branch original
            indicator.setText(MessageBundle.message("progress.returning"));
            indicator.setFraction(currentStep / totalSteps);

            GitCommandResult returnResult = checkout(repository, originalBranch);
            if (!returnResult.success()) {
                NotificationHelper.notifyError(
                        project,
                        NotificationHelper.DEFAULT_TITLE,
                        MessageBundle.message("error.return", originalBranch,
                                String.join("\n", returnResult.getErrorOutput())));
                allSuccessful = false;
            }

            // 5. Deletar branch source se necessário
            if (deleteSourceBranch && allSuccessful) {
                indicator.setText(MessageBundle.message("progress.deleting.source"));

                // Verificar se podemos usar a branch atual
                if (originalBranch.equals(sourceBranch)) {
                    NotificationHelper.notifyWarning(
                            project,
                            NotificationHelper.DEFAULT_TITLE,
                            MessageBundle.message("error.current.branch", sourceBranch));
                } else {
                    // Verifica se há uma branch remota correspondente
                    GitRemoteBranch remoteBranch = findRemoteBranch(repository, sourceBranch);

                    // Deleta a branch local
                    GitCommandResult deleteResult = deleteBranch(repository, sourceBranch);
                    if (!deleteResult.success()) {
                        NotificationHelper.notifyWarning(
                                project,
                                NotificationHelper.DEFAULT_TITLE,
                                MessageBundle.message("error.delete.source", sourceBranch,
                                        String.join("\n", deleteResult.getErrorOutput())));
                    } else if (remoteBranch != null) {
                        // Se tem branch remota, deleta ela também
                        GitCommandResult deleteRemoteResult = deleteRemoteBranch(repository, remoteBranch);
                        if (!deleteRemoteResult.success()) {
                            NotificationHelper.notifyWarning(
                                    project,
                                    NotificationHelper.DEFAULT_TITLE,
                                    MessageBundle.message("error.delete.remote",
                                            remoteBranch.getNameForRemoteOperations(),
                                            String.join("\n", deleteRemoteResult.getErrorOutput())));
                        }
                    }
                }
            }

            // Finaliza o progresso
            indicator.setText(MessageBundle.message("progress.updating.refs"));

            // Executar fetch para atualizar referências
            GitLineHandler fetchHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.FETCH);
            fetchHandler.addParameters("--all", "--prune");
            git.runCommand(fetchHandler);

            // Forçar atualização do repositório
            repository.update();

            indicator.setText(MessageBundle.message("progress.completed"));
            indicator.setFraction(1.0);

            // Notificar resultados
            StringBuilder summary = new StringBuilder();

            if (!successfulMerges.isEmpty()) {
                summary.append(MessageBundle.message("summary.successful.merges",
                        String.join(", ", successfulMerges)));
            }

            if (!failedMerges.isEmpty()) {
                if (!summary.isEmpty())
                    summary.append("\n\n");
                summary.append(MessageBundle.message("summary.failed.merges",
                        String.join(", ", failedMerges)));
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
            NotificationHelper.notifyError(
                    project,
                    NotificationHelper.DEFAULT_TITLE,
                    e);
            future.complete(false);
        }
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
    private boolean hasPendingChanges(@NotNull GitRepository repository, @NotNull String sourceBranch) {
        GitLineHandler diffHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.DIFF);
        diffHandler.addParameters(sourceBranch, "--name-only");
        GitCommandResult diffResult = git.runCommand(diffHandler);

        // Retorna true se houver pelo menos uma linha não vazia na saída do diff
        return diffResult.getOutput().stream().anyMatch(line -> !line.trim().isEmpty());
    }

    /**
     * Faz o checkout para uma branch específica
     */
    private GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
        handler.addParameters(branchName);
        return git.runCommand(handler);
    }

    /**
     * Executa o merge com a branch source
     */
    private GitCommandResult merge(@NotNull GitRepository repository, @NotNull String sourceBranch,
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
    private GitCommandResult push(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
        handler.addParameters("origin", branchName);
        return git.runCommand(handler);
    }

    /**
     * Deleta uma branch local
     */
    private GitCommandResult deleteBranch(@NotNull GitRepository repository, @NotNull String branchName) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.BRANCH);
        handler.addParameters("-D", branchName);
        return git.runCommand(handler);
    }

    /**
     * Encontra a branch remota correspondente a uma branch local
     */
    private GitRemoteBranch findRemoteBranch(@NotNull GitRepository repository, @NotNull String localBranchName) {
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
    private GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository,
            @NotNull GitRemoteBranch remoteBranch) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
        handler.addParameters("origin", "--delete", remoteBranch.getNameForRemoteOperations());
        return git.runCommand(handler);
    }
}