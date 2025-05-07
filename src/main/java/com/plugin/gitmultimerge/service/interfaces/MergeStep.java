package com.plugin.gitmultimerge.service.interfaces;

import com.plugin.gitmultimerge.service.MergeContext;

/**
 * Representa uma etapa atômica do fluxo de multi-merge.
 * Cada implementação executa uma ação sobre o contexto do merge.
 */
public interface MergeStep {
    /**
     * Executa a etapa sobre o contexto do merge.
     *
     * @param context Contexto do merge contendo informações do fluxo.
     * @return true para continuar o fluxo, false para interromper.
     */
    boolean execute(MergeContext context);
}