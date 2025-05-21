package com.plugin.gitmultimerge.service;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode;
import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.VcsRefreshUtil;

public class UpdateChangeListManagerStep {
    public static void update(Project project) {
        ChangeListManager.getInstance(project).invokeAfterUpdate(
                () -> VcsRefreshUtil.refreshVcsAndCommitPanel(project),
                InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
                MessageBundle.message("progress.updating.conflicts"),
                ModalityState.defaultModalityState());
    }
}