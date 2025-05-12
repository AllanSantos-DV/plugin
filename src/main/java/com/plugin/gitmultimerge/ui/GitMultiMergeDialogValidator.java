package com.plugin.gitmultimerge.ui;

import com.plugin.gitmultimerge.util.MessageBundle;

import java.util.List;

public class GitMultiMergeDialogValidator {
    /**
     * Valida as branches target selecionadas.
     * 
     * @param sourceBranch   branch source
     * @param targetBranches lista de branches target
     * @return mensagem de erro, ou null se válido
     */
    public static String validateTargetBranches(String sourceBranch, List<String> targetBranches) {
        if (targetBranches == null || targetBranches.isEmpty()) {
            return MessageBundle.message("error.no.targets");
        }
        if (targetBranches.contains(sourceBranch)) {
            return MessageBundle.message("error.branch.protected", sourceBranch);
        }
        return null;
    }
}