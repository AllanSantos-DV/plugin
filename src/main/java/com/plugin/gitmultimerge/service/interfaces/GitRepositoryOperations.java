package com.plugin.gitmultimerge.service.interfaces;

import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRemoteBranch;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Operações Git de baixo nível sobre o repositório.
 * Permite executar comandos essenciais para o fluxo de merge.
 */
public interface GitRepositoryOperations {
        /**
         * Faz checkout para a branch especificada.
         *
         * @param repository Repositório Git alvo.
         * @param branchName Nome da branch para checkout.
         * @return Resultado do comando Git.
         */
        GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String branchName);

        /**
         * Executa o merge da branch source na branch atual.
         *
         * @param repository    Repositório Git alvo.
         * @param sourceBranch  Nome da branch source.
         * @param squash        Se true, faz squash dos commits.
         * @param commitMessage Mensagem de commit para squash.
         * @return Resultado do comando Git.
         */
        GitCommandResult merge(@NotNull GitRepository repository, @NotNull String sourceBranch, boolean squash,
                        String commitMessage);

        /**
         * Realiza push da branch especificada para o remote, com opção de setUpstream.
         *
         * @param repository  Repositório Git alvo.
         * @param branchName  Nome da branch para push.
         * @param setUpstream Se true, adiciona o parâmetro -u para criar e rastrear a
         *                    branch remota.
         * @return Resultado do comando Git.
         */
        GitCommandResult push(@NotNull GitRepository repository, @NotNull String branchName, boolean setUpstream);

        /**
         * Realiza o pull da branch especificada.
         *
         * @param repository Repositório Git alvo.
         * @param branchName Nome da branch para pull.
         * @return Resultado do comando Git.
         */
        GitCommandResult pull(@NotNull GitRepository repository, @NotNull String branchName);

        /**
         * Delete a branch local especificada.
         *
         * @param repository Repositório Git alvo.
         * @param branchName Nome da branch a ser removida.
         * @return Resultado do comando Git.
         */
        GitCommandResult deleteBranch(@NotNull GitRepository repository, @NotNull String branchName);

        /**
         * Busca a branch remota correspondente a uma branch local.
         *
         * @param repository      Repositório Git alvo.
         * @param localBranchName Nome da branch local.
         * @return Branch remota correspondente, ou null se não encontrada.
         */
        GitRemoteBranch findRemoteBranch(@NotNull GitRepository repository, @NotNull String localBranchName);

        /**
         * Delete a branch remota especificada.
         *
         * @param repository   Repositório Git alvo.
         * @param remoteBranch Branch remota a ser removida.
         * @return Resultado do comando Git.
         */
        GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository, @NotNull GitRemoteBranch remoteBranch);

        /**
         * Verifica se há alterações pendentes entre a branch atual e a branch de
         * origem.
         *
         * @param repository   Repositório Git alvo.
         * @param sourceBranch Nome da branch de origem.
         * @return true se há alterações pendentes, false caso contrário.
         */
        boolean hasPendingChanges(@NotNull GitRepository repository, @NotNull String sourceBranch);

        /**
         * Executa git fetch --all para atualizar referências remotas.
         *
         * @param repository Repositório Git alvo.
         */
        void fetchAll(@NotNull GitRepository repository, boolean deletedBranches);

        /**
         * Executa um commit avulso com a mensagem fornecida.
         *
         * @param repository    Repositório Git alvo.
         * @param commitMessage Mensagem de commit.
         * @return Resultado do comando Git.
         */
        GitCommandResult commit(@NotNull GitRepository repository, String commitMessage);

        /**
         * Verifica se a branch target contém todos os commits da branch source.
         *
         * @param repository   Repositório Git alvo.
         * @param targetBranch Nome da branch target.
         * @param sourceBranch Nome da branch source.
         * @return true se a target contém todos os commits da source, false caso
         *         contrário.
         */
        boolean isTargetUpToDateWithSource(@NotNull GitRepository repository, @NotNull String targetBranch,
                        @NotNull String sourceBranch);

        /**
         * Retorna o conjunto de arquivos em conflito (unmerged) no repositório.
         * Utiliza 'git diff --name-only --diff-filter=U' para detecção robusta.
         *
         * @param repository Repositório Git alvo.
         * @return Conjunto de caminhos relativos dos arquivos em conflito.
         */
        Set<VirtualFile> getConflictedFiles(@NotNull GitRepository repository);

        /**
         * Adiciona os arquivos informados ao index (git add).
         * 
         * @param repository Repositório Git alvo.
         * @param files      Lista de arquivos a serem adicionados ao index.
         */
        void addFilesToIndex(@NotNull GitRepository repository, @NotNull List<VirtualFile> files);

        /**
         * Aborta o merge em andamento (git merge --abort).
         * 
         * @param repository Repositório Git alvo.
         */
        void abortMerge(@NotNull GitRepository repository);
}