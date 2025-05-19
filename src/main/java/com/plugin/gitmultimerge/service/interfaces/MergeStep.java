package com.plugin.gitmultimerge.service.interfaces;

import com.plugin.gitmultimerge.service.MergeContext;
import com.plugin.gitmultimerge.service.StepResult;

/**
 * Representa uma etapa atômica do fluxo de multi merge.
 * Cada implementação executa uma ação sobre o contexto do merge.
 */
public interface MergeStep {
    /**
     * Executa a etapa sobre o contexto do merge.
     *
     * @param context Contexto do merge contendo informações do fluxo.
     * @return Resultado @see {@link StepResult} da execução da etapa.
     */
    StepResult execute(MergeContext context);

    /**
     * Executa a etapa de falha sobre o contexto do merge.
     * @param context Contexto do merge contendo informações do fluxo.
     * @return Resultado @see {@link StepResult} da execução da etapa.
     */
    StepResult failure(MergeContext context);

    /**
     * Executa a etapa de sucesso sobre o contexto do merge.
     * @param context Contexto do merge contendo informações do fluxo.
     */
    void success(MergeContext context);
}