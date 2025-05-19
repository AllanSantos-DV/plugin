package com.plugin.gitmultimerge.service;

import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.commands.GitCommandResult;

public class ResultFailStep {
    private final GitCommandResult result;
    private final String message;

    public ResultFailStep(GitCommandResult result, String message) {
        this.result = result;
        this.message = message;
    }

    public StepResult checkConflict(MergeContext context){
        boolean conflict = result.getOutput().stream().anyMatch(line -> line.toUpperCase().contains("CONFLICT"));
        StepResult stepResult = conflict ? StepResult.CONFLICT : StepResult.FAILURE;

        if (stepResult == StepResult.CONFLICT) {
            NotificationHelper.notifyWarning(
                    context.project,
                    NotificationHelper.DEFAULT_TITLE,
                    message);
        }

        return stepResult;
    }
}
