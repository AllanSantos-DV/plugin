package com.plugin.gitmultimerge.service;

/**
 * Interface para uma etapa do processo de multi-merge.
 * Cada implementação executa uma ação sobre o contexto e retorna true para
 * continuar ou false para interromper o fluxo.
 */
public interface MergeStep {
    /**
     * Executa uma etapa do processo de merge.
     * 
     * @param context Contexto compartilhado do merge.
     * @return true para continuar, false para interromper o fluxo.
     */
    boolean execute(MergeContext context);
}