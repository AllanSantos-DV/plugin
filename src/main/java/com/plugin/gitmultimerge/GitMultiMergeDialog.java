package com.plugin.gitmultimerge;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import git4idea.GitLocalBranch;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Diálogo para selecionar branches e configurar opções para o multi-merge.
 */
public class GitMultiMergeDialog extends DialogWrapper {
    private final Project project;
    private final GitRepository repository;
    private final Git git;

    private JBList<String> sourceBranchList;
    private JBList<String> targetBranchList;
    private JBCheckBox squashCheckBox;
    private JBCheckBox deleteSourceCheckBox;
    private JBTextField mergeCommitMessageField;

    public GitMultiMergeDialog(@NotNull Project project, @NotNull GitRepository repository) {
        super(project);
        this.project = project;
        this.repository = repository;
        this.git = Git.getInstance();

        setTitle("Git Multi Merge");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // Configuração do painel principal
        c.fill = GridBagConstraints.BOTH;
        c.insets = JBUI.insets(5);

        // Lista de branches source
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.weighty = 0;
        panel.add(new JBLabel("Branch source:"), c);

        c.gridy = 1;
        c.weighty = 1.0;
        sourceBranchList = new JBList<>(getBranchNames().toArray(new String[0]));
        sourceBranchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JBScrollPane sourceScrollPane = new JBScrollPane(sourceBranchList);
        panel.add(sourceScrollPane, c);

        // Lista de branches target
        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 0;
        panel.add(new JBLabel("Branches Target (máx. 5):"), c);

        c.gridy = 1;
        c.weighty = 1.0;
        targetBranchList = new JBList<>(getBranchNames().toArray(new String[0]));
        targetBranchList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Limite a seleção para no máximo 5 branches target
        targetBranchList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && targetBranchList.getSelectedIndices().length > 5) {
                targetBranchList.setSelectedIndices(
                        java.util.Arrays.copyOf(targetBranchList.getSelectedIndices(), 5));
            }
        });

        JBScrollPane targetScrollPane = new JBScrollPane(targetBranchList);
        panel.add(targetScrollPane, c);

        // Opções adicionais
        JPanel optionsPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        squashCheckBox = new JBCheckBox("Squash commits");
        deleteSourceCheckBox = new JBCheckBox("Deletar branch source após o merge");

        // Painel para mensagem de commit
        JPanel commitMessagePanel = new JPanel(new BorderLayout(5, 5));
        commitMessagePanel.add(new JBLabel("Mensagem de merge (opcional):"), BorderLayout.NORTH);
        mergeCommitMessageField = new JBTextField();
        mergeCommitMessageField.setEnabled(true);
        commitMessagePanel.add(mergeCommitMessageField, BorderLayout.CENTER);

        optionsPanel.add(squashCheckBox);
        optionsPanel.add(deleteSourceCheckBox);
        optionsPanel.add(commitMessagePanel);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weighty = 0;
        panel.add(optionsPanel, c);

        // Tamanho do diálogo
        panel.setPreferredSize(new Dimension(600, 400));

        return panel;
    }

    /**
     * Obtém a lista de nomes de branches no repositório.
     */
    private List<String> getBranchNames() {
        List<String> branchNames = new ArrayList<>();
        Collection<GitLocalBranch> localBranches = repository.getBranches().getLocalBranches();

        for (GitLocalBranch branch : localBranches) {
            branchNames.add(branch.getName());
        }

        return branchNames;
    }

    @Override
    protected void doOKAction() {
        // Validação de entrada
        String sourceBranch = sourceBranchList.getSelectedValue();
        List<String> targetBranches = targetBranchList.getSelectedValuesList();

        if (sourceBranch == null) {
            Messages.showErrorDialog(project, "Selecione uma branch source", "Erro");
            return;
        }

        if (targetBranches.isEmpty()) {
            Messages.showErrorDialog(project, "Selecione pelo menos uma branch target", "Erro");
            return;
        }

        if (targetBranches.contains(sourceBranch)) {
            Messages.showErrorDialog(project,
                    "A branch source não pode ser selecionada como target", "Erro");
            return;
        }

        // Inicia o processo de merge em background com barra de progresso
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Git multi merge", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                performMergeOperation(sourceBranch, targetBranches, indicator);
            }
        });

        super.doOKAction();
    }

    /**
     * Executa as operações de merge para todas as branches target.
     */
    private void performMergeOperation(String sourceBranch, List<String> targetBranches,
            ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);

        boolean squash = squashCheckBox.isSelected();
        boolean deleteSource = deleteSourceCheckBox.isSelected();
        String mergeMessage = mergeCommitMessageField.getText();
        if (mergeMessage == null || mergeMessage.trim().isEmpty()) {
            mergeMessage = "Merge branch '" + sourceBranch + "'";
        }

        List<String> successfulMerges = new ArrayList<>();
        List<String> failedMerges = new ArrayList<>();
        List<String> hookErrors = new ArrayList<>();

        double progressStep = 1.0 / targetBranches.size();
        double currentProgress = 0.0;

        // Executa o merge para cada branch target
        for (String targetBranch : targetBranches) {
            indicator.setText("Merge de " + sourceBranch + " para " + targetBranch);

            try {
                // Checkout para a branch target
                GitLineHandler checkoutHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
                checkoutHandler.addParameters(targetBranch);
                GitCommandResult checkoutResult = git.runCommand(checkoutHandler);

                if (!checkoutResult.success()) {
                    throw new VcsException("Falha ao fazer checkout para " + targetBranch + ": " +
                            checkoutResult.getErrorOutputAsJoinedString());
                }

                // Executa o merge
                GitLineHandler mergeHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);
                if (squash) {
                    mergeHandler.addParameters("--squash");
                }
                if (!mergeMessage.isEmpty()) {
                    mergeHandler.addParameters("-m", mergeMessage);
                }
                mergeHandler.addParameters(sourceBranch);
                GitCommandResult mergeResult = git.runCommand(mergeHandler);

                if (!mergeResult.success()) {
                    // Verifica se a mensagem de erro contém indicações de hook
                    boolean hookError = mergeResult.getErrorOutputAsJoinedString().contains("hook");

                    if (hookError) {
                        hookErrors.add("Possível hook Git impediu o merge para " +
                                targetBranch + ": " + mergeResult.getErrorOutputAsJoinedString());
                    } else {
                        throw new VcsException("Falha ao fazer merge para " + targetBranch + ": " +
                                mergeResult.getErrorOutputAsJoinedString());
                    }

                    failedMerges.add(targetBranch);
                } else {
                    successfulMerges.add(targetBranch);
                }
            } catch (VcsException e) {
                failedMerges.add(targetBranch);
                VcsNotifier.getInstance(project).notifyError(
                        "Git Multi Merge",
                        "Erro ao processar " + targetBranch + ": " + e.getMessage(),
                        "");
            }

            currentProgress += progressStep;
            indicator.setFraction(currentProgress);
        }

        // Deleta a branch source se solicitado e se todos os merges foram bem-sucedidos
        if (deleteSource && !successfulMerges.isEmpty() && failedMerges.isEmpty()) {
            try {
                // Checkout para uma branch diferente (a primeira branch de sucesso)
                GitLineHandler checkoutHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
                checkoutHandler.addParameters(successfulMerges.get(0));
                GitCommandResult checkoutResult = git.runCommand(checkoutHandler);

                if (!checkoutResult.success()) {
                    throw new VcsException("Falha ao fazer checkout para deletar a branch source: " +
                            checkoutResult.getErrorOutputAsJoinedString());
                }

                // Deleta a branch
                GitLineHandler deleteHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.BRANCH);
                deleteHandler.addParameters("-D", sourceBranch); // Forçar exclusão com -D
                GitCommandResult deleteResult = git.runCommand(deleteHandler);

                if (!deleteResult.success()) {
                    throw new VcsException("Falha ao excluir a branch " + sourceBranch + ": " +
                            deleteResult.getErrorOutputAsJoinedString());
                }
            } catch (VcsException e) {
                VcsNotifier.getInstance(project).notifyError(
                        "Git Multi Merge",
                        "Erro ao deletar branch source: " + e.getMessage(),
                        "");
            }
        }

        // Notifica o resultado
        StringBuilder message = new StringBuilder();
        if (!successfulMerges.isEmpty()) {
            message.append("Merges realizados com sucesso para: ")
                    .append(String.join(", ", successfulMerges))
                    .append("\n");
        }

        if (!failedMerges.isEmpty()) {
            message.append("Falha nos merges para: ")
                    .append(String.join(", ", failedMerges))
                    .append("\n");
        }

        if (!hookErrors.isEmpty()) {
            message.append("\nErros de hooks:\n")
                    .append(String.join("\n", hookErrors));
        }

        if (deleteSource && !successfulMerges.isEmpty() && failedMerges.isEmpty()) {
            message.append("\nBranch source ").append(sourceBranch).append(" foi deletada.");
        }

        if (failedMerges.isEmpty() && hookErrors.isEmpty()) {
            VcsNotifier.getInstance(project).notifySuccess(
                    "Git Multi Merge",
                    message.toString(),
                    "");
        } else {
            VcsNotifier.getInstance(project).notifyImportantWarning(
                    "Git Multi Merge",
                    message.toString(),
                    "");
        }
    }
}