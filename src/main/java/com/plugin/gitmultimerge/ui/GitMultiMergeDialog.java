package com.plugin.gitmultimerge.ui;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.plugin.gitmultimerge.service.interfaces.GitMultiMergeService;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Diálogo para selecionar branches e configurar opções para o multi-merge.
 * Usa GitMultiMergeService para implementar operações Git.
 */
public class GitMultiMergeDialog extends DialogWrapper {
    private final Project project;
    private final GitRepository repository;
    private final GitMultiMergeService gitService;

    private JBCheckBox squashCheckBox;
    private JBCheckBox deleteSourceCheckBox;
    private JBCheckBox pushAfterMergeCheckBox;
    private JBTextField mergeCommitMessageField;
    private final List<String> allBranchNames;
    private BranchTreePanel branchTreePanel;
    private SourceBranchPanel sourceBranchPanel;

    public GitMultiMergeDialog(@NotNull Project project, @NotNull GitRepository repository) {
        super(project);
        this.project = project;
        this.repository = repository;
        // Obtém o serviço Git Multi Merge do registro de serviços do projeto
        this.gitService = project.getService(GitMultiMergeService.class);
        // Usa o serviço para obter os nomes das branches
        this.allBranchNames = gitService.getBranchNames(repository);

        setTitle(MessageBundle.message("dialog.title"));

        // Botão OK inicialmente desativado (até verificarmos se não há alterações)
        setOKActionEnabled(false);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createSourcePanel(), c);

        c.gridy = 1;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        panel.add(createTargetPanel(), c);

        c.gridy = 2;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createOptionsPanel(), c);

        panel.setPreferredSize(new Dimension(450, 550));
        return panel;
    }

    /**
     * Cria o painel de seleção da branch source.
     */
    private JPanel createSourcePanel() {
        String currentBranch = repository.getCurrentBranchName();
        this.sourceBranchPanel = new SourceBranchPanel(allBranchNames, currentBranch, this::updateTargetTree);
        return sourceBranchPanel;
    }

    /**
     * Cria o painel de seleção das branches target com campo de busca.
     */
    private JPanel createTargetPanel() {
        JPanel targetMainPanel = new JPanel(new BorderLayout(0, 5));
        targetMainPanel.add(new JBLabel(MessageBundle.message("target.branches.label")), BorderLayout.NORTH);
        String selectedBranch = sourceBranchPanel.getSelectedBranch();
        branchTreePanel = new BranchTreePanel(allBranchNames, selectedBranch);
        branchTreePanel.addTreeSelectionListener(e -> {
            List<String> selected = branchTreePanel.getSelectedBranches();
            boolean hasSelectedLeaf = selected != null && !selected.isEmpty();
            boolean warningExists = sourceBranchPanel.hasWarning();
            setOKActionEnabled(hasSelectedLeaf && !warningExists);
        });
        targetMainPanel.add(branchTreePanel, BorderLayout.CENTER);
        return targetMainPanel;
    }

    /**
     * Cria o painel de opções adicionais (checkboxes e mensagem de commit).
     */
    private JPanel createOptionsPanel() {
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints optConstraints = new GridBagConstraints();
        optConstraints.fill = GridBagConstraints.HORIZONTAL;
        optConstraints.weightx = 1.0;
        optConstraints.gridx = 0;
        optConstraints.insets = JBUI.insets(2);

        squashCheckBox = new JBCheckBox(MessageBundle.message("options.squash.commits"));
        optConstraints.gridy = 0;
        optionsPanel.add(squashCheckBox, optConstraints);

        deleteSourceCheckBox = new JBCheckBox(MessageBundle.message("options.delete.branch.after"));
        optConstraints.gridy = 1;
        optionsPanel.add(deleteSourceCheckBox, optConstraints);

        pushAfterMergeCheckBox = new JBCheckBox(MessageBundle.message("options.push.after.merge"));
        pushAfterMergeCheckBox.setSelected(true);
        optConstraints.gridy = 2;
        optionsPanel.add(pushAfterMergeCheckBox, optConstraints);

        JPanel commitMessagePanel = new JPanel(new BorderLayout(5, 2));
        commitMessagePanel.add(new JBLabel(MessageBundle.message("options.commit.message")), BorderLayout.NORTH);
        mergeCommitMessageField = new JBTextField();
        mergeCommitMessageField.setEnabled(true);
        commitMessagePanel.add(mergeCommitMessageField, BorderLayout.CENTER);

        optConstraints.gridy = 3;
        optConstraints.insets = JBUI.insets(5, 2, 2, 2);
        optionsPanel.add(commitMessagePanel, optConstraints);

        return optionsPanel;
    }

    /**
     * Verifica se a branch source selecionada tenha alterações não enviadas de
     * forma assíncrona.
     * Esta abordagem evita bloquear a EDT.
     */
    private void checkSourceBranchUncommittedChangesAsync() {
        setOKActionEnabled(false);
        sourceBranchPanel.clearWarning();
        CompletableFuture<Boolean> validationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return gitService.hasUncommittedChanges(repository);
            } catch (Exception e) {
                return true;
            }
        });
        validationFuture.thenAcceptAsync(hasChanges -> {
            if (hasChanges) {
                String msg = MessageBundle.message("error.source.uncommitted.changes.message");
                sourceBranchPanel.setWarning("<html>" + msg.replace("\\n", "<br>") + "</html>");
                setOKActionEnabled(false);
            } else {
                sourceBranchPanel.clearWarning();
            }
        }, SwingUtilities::invokeLater);
    }

    /**
     * Atualiza a árvore de branches target com base na branch source selecionada e
     * texto de busca. Expande apenas os grupos relevantes.
     */
    private void updateTargetTree() {
        String sourceBranch = sourceBranchPanel.getSelectedBranch();
        branchTreePanel.setSourceBranch(sourceBranch);
    }

    /**
     * Retorna a lista de branches target selecionadas a partir do JTree.
     */
    private List<String> getSelectedTargetBranches() {
        return branchTreePanel.getSelectedBranches();
    }

    /**
     * Verifica se há alterações não enviadas durante a operação de OK.
     * Esta verificação é feita de forma síncrona, pois o utilizador já está a aguardar.
     */
    @Override
    protected void doOKAction() {
        String sourceBranch = sourceBranchPanel.getSelectedBranch();
        if (!allBranchNames.contains(sourceBranch)) {
            Messages.showErrorDialog(project, MessageBundle.message("error.no.source"),
                    MessageBundle.message("dialog.title"));
            return;
        }
        List<String> targetBranches = getSelectedTargetBranches();
        if (!validateTargetBranches(sourceBranch, targetBranches))
            return;
        startMergeProcess(sourceBranch, targetBranches);
        super.doOKAction();
    }

    /**
     * Valida as branches target selecionadas.
     */
    private boolean validateTargetBranches(String sourceBranch, List<String> targetBranches) {
        String message = GitMultiMergeDialogValidator.validateTargetBranches(sourceBranch, targetBranches);
        if (message != null) {
            Messages.showErrorDialog(project, message, MessageBundle.message("dialog.title"));
            return false;
        }
        return true;
    }

    /**
     * Inicia o processo de merge em background.
     */
    private void startMergeProcess(String sourceBranch, List<String> targetBranches) {
        boolean squash = squashCheckBox.isSelected();
        boolean deleteSource = deleteSourceCheckBox.isSelected();
        boolean pushAfterMerge = pushAfterMergeCheckBox.isSelected();
        String mergeMessage = mergeCommitMessageField.getText();

        ProgressManager.getInstance()
                .run(new Task.Backgroundable(project, MessageBundle.message("dialog.title"), true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        CompletableFuture<Boolean> future = gitService.performMerge(
                                repository,
                                sourceBranch,
                                targetBranches,
                                squash,
                                pushAfterMerge,
                                deleteSource,
                                mergeMessage,
                                indicator);
                        future.exceptionally(throwable -> {
                            NotificationHelper.notifyError(project, NotificationHelper.DEFAULT_TITLE, throwable);
                            return false;
                        });
                    }
                });
    }

    @Override
    public void show() {
        checkSourceBranchUncommittedChangesAsync();
        super.show();
    }
}