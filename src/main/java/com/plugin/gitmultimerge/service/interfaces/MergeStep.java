package com.plugin.gitmultimerge.service.interfaces;

import com.plugin.gitmultimerge.service.MergeContext;

/**
 * Representa uma etapa atômica do fluxo de multi-merge.
 * Cada implementação executa uma ação sobre o contexto.
 */
public interface MergeStep {
    /**
     * Executa a etapa sobre o contexto do merge. Retorna true para continuar o
     * fluxo.
     */
    boolean execute(MergeContext context);
}