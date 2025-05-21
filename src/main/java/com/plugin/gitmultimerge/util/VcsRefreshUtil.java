package com.plugin.gitmultimerge.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;

/**
 * Utility class to force a refresh of the VCS state and the Commit panel in IntelliJ IDEA.
 * This ensures that all changes and statuses are visually updated for the user after Git operations.
 */
public class VcsRefreshUtil {
    /**
     * Forces a refresh of the VCS and Commit panel for the given project.
     *
     * @param project the IntelliJ project instance
     */
    public static void refreshVcsAndCommitPanel(Project project) {
        VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
    }
} 