package com.plugin.gitmultimerge.service;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.merge.GitMergeUtil;
import com.intellij.openapi.vcs.merge.MergeData;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.merge.MergeRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.plugin.gitmultimerge.service.interfaces.GitRepositoryOperations;
import git4idea.repo.GitRepository;
import com.intellij.openapi.project.Project;
import com.intellij.diff.merge.MergeResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MergeConflictResolutionStep {
    private final GitRepositoryOperations service;

    public MergeConflictResolutionStep(GitRepositoryOperations service) {
        this.service = service;
    }

    public boolean execute(MergeContext context) {
        Project project = context.project;
        GitRepository repository = context.repository;
        // Lista de arquivos em conflito antes do merge tool
        Set<VirtualFile> initialConflicted = service.getConflictedFiles(repository);
        List<VirtualFile> resolvedFiles = new ArrayList<>();

        boolean cancelled = false;
        for (VirtualFile file : initialConflicted) {
            if (file != null && file.isValid()) {
                try {
                    MergeData mergeData = GitMergeUtil.loadMergeData(
                            project,
                            repository.getRoot(),
                            VcsUtil.getFilePath(file),
                            false);
                    List<byte[]> contents = Arrays.asList(
                            mergeData.ORIGINAL,
                            mergeData.CURRENT,
                            mergeData.LAST);
                    final boolean[] fileCancelled = { false };
                    MergeRequest request = DiffRequestFactory.getInstance().createMergeRequest(
                            project,
                            file,
                            contents,
                            MessageBundle.message("merge.dialog.title", file.getName()),
                            Arrays.asList(
                                    MessageBundle.message("merge.label.base"),
                                    MessageBundle.message("merge.label.local"),
                                    MessageBundle.message("merge.label.remote")),
                            result -> {
                                if (result == MergeResult.RESOLVED
                                        || result == MergeResult.LEFT
                                        || result == MergeResult.RIGHT) {
                                    resolvedFiles.add(file);
                                } else if (result == MergeResult.CANCEL) {
                                    fileCancelled[0] = true;
                                }
                            });
                    ApplicationManager.getApplication().invokeAndWait(
                            () -> DiffManager.getInstance().showMerge(project, request),
                            ModalityState.defaultModalityState());
                    if (fileCancelled[0]) {
                        cancelled = true;
                        break;
                    }
                } catch (ProcessCanceledException pce) {
                    throw pce;
                } catch (Exception e) {
                    context.errorMessage = MessageBundle.message("error.merge", file.getName(), e.getMessage());
                    return false;
                }
            }
        }

        // Se o usuário cancelou o merge, aborta o merge
        if (cancelled) {
            service.abortMerge(repository);
            UpdateChangeListManagerStep.update(project);
            context.errorMessage = MessageBundle.message("error.merge.cancelled.rollback");
            context.allSuccessful = false;
            return false;
        }

        // Marca como resolvidos apenas os arquivos realmente tratados
        if (!resolvedFiles.isEmpty()) {
            try {
                service.addFilesToIndex(repository, resolvedFiles);
                UpdateChangeListManagerStep.update(project);
            } catch (Exception e) {
                context.errorMessage = MessageBundle.message("error.mark.resolved", e.getMessage());
                return false;
            }
        }

        // Commit automático
        boolean commitOk = new AutoCommitStep(service).autoCommit(context);
        if (commitOk) {
            Set<VirtualFile> finalConflicted = service.getConflictedFiles(repository);
            if (!finalConflicted.isEmpty()) {
                context.errorMessage = MessageBundle.message("error.unresolved.conflicts");
                context.allSuccessful = false;
                return false;
            }
        }

        // Atualiza lista de conflitos após marcar como resolvido
        context.allSuccessful = commitOk;
        return commitOk;
    }
}