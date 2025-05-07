package com.plugin.gitmultimerge.ui;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.plugin.gitmultimerge.service.GitMultiMergeService;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
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

    private JComboBox<String> sourceBranchComboBox;
    private JBList<String> targetBranchList;
    private JBTextField searchField;
    private JBCheckBox squashCheckBox;
    private JBCheckBox deleteSourceCheckBox;
    private JBCheckBox pushAfterMergeCheckBox;
    private JBTextField mergeCommitMessageField;
    private DefaultListModel<String> targetListModel;
    private final List<String> allBranchNames;
    private JBLabel warningLabel; // Novo label para mostrar aviso de alterações não commitadas

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
        JPanel sourcePanel = new JPanel(new BorderLayout(0, 5));
        sourcePanel.add(new JBLabel(MessageBundle.message("source.branch.label")), BorderLayout.NORTH);

        JPanel comboPanel = new JPanel(new BorderLayout());
        String currentBranch = repository.getCurrentBranchName();
        sourceBranchComboBox = new ComboBox<>(allBranchNames.toArray(new String[0]));
        sourceBranchComboBox.setSelectedItem(currentBranch);
        comboPanel.add(sourceBranchComboBox, BorderLayout.CENTER);
        sourcePanel.add(comboPanel, BorderLayout.CENTER);

        warningLabel = new JBLabel();
        warningLabel.setForeground(JBColor.RED);
        warningLabel.setVisible(false);
        sourcePanel.add(warningLabel, BorderLayout.SOUTH);

        sourceBranchComboBox.addActionListener(e -> updateTargetList());

        return sourcePanel;
    }

    /**
     * Cria o painel de seleção das branches target com campo de busca.
     */
    private JPanel createTargetPanel() {
        JPanel targetMainPanel = new JPanel(new BorderLayout(0, 5));
        targetMainPanel.add(new JBLabel(MessageBundle.message("target.branches.label")), BorderLayout.NORTH);

        JPanel targetPanel = new JPanel(new BorderLayout(0, 5));
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JBTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);
        targetPanel.add(searchPanel, BorderLayout.NORTH);

        targetListModel = new DefaultListModel<>();
        targetBranchList = new JBList<>(targetListModel);
        targetBranchList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        updateTargetList();

        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateTargetList();
            }
        });

        targetBranchList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && targetBranchList.getSelectedIndices().length > 5) {
                targetBranchList.setSelectedIndices(
                        java.util.Arrays.copyOf(targetBranchList.getSelectedIndices(), 5));
            }
        });

        JBScrollPane targetScrollPane = new JBScrollPane(targetBranchList);
        targetPanel.add(targetScrollPane, BorderLayout.CENTER);
        targetMainPanel.add(targetPanel, BorderLayout.CENTER);
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
     * Verifica se a branch source selecionada tem alterações não commitadas de
     * forma assíncrona.
     * Esta abordagem evita bloquear a EDT.
     */
    private void checkSourceBranchUncommittedChangesAsync() {
        String selectedBranch = (String) sourceBranchComboBox.getSelectedItem();
        if (selectedBranch == null) {
            setOKActionEnabled(false);
            return;
        }

        // Botão OK começa desabilitado e só é habilitado se não houver alterações
        setOKActionEnabled(false);
        warningLabel.setVisible(false);

        // Criar um CompletableFuture para a validação
        CompletableFuture<Boolean> validationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return gitService.hasUncommittedChanges(repository);
            } catch (Exception e) {
                return true; // Em caso de erro, consideramos que há alterações
            }
        });

        // Processar o resultado na EDT
        validationFuture.thenAcceptAsync(hasChanges -> {
            if (hasChanges) {
                warningLabel.setText(MessageBundle.message("error.source.uncommitted.changes.message"));
                warningLabel.setVisible(true);
                setOKActionEnabled(false);
            } else {
                warningLabel.setVisible(false);
                setOKActionEnabled(true);
            }
        }, SwingUtilities::invokeLater);
    }

    /**
     * Atualiza a lista de branches target com base na branch source selecionada e
     * texto de busca.
     */
    private void updateTargetList() {
        String sourceBranch = (String) sourceBranchComboBox.getSelectedItem();
        String searchText = searchField.getText();

        targetListModel.clear();

        for (String branch : allBranchNames) {
            // Não incluir a branch source na lista de targets
            if (!branch.equals(sourceBranch) &&
                    (searchText == null || searchText.isEmpty() ||
                            branch.toLowerCase().contains(searchText.toLowerCase()))) {
                targetListModel.addElement(branch);
            }
        }
    }

    /**
     * Verifica se há alterações não commitadas durante a operação de OK.
     * Esta verificação é feita de forma síncrona pois o usuário já está aguardando.
     */
    @Override
    protected void doOKAction() {
        String sourceBranch = (String) sourceBranchComboBox.getSelectedItem();
        List<String> targetBranches = targetBranchList.getSelectedValuesList();

        if (!validateSourceBranch(sourceBranch))
            return;
        if (!validateTargetBranches(sourceBranch, targetBranches))
            return;

        startMergeProcess(sourceBranch, targetBranches);
        super.doOKAction();
    }

    /**
     * Valida a branch source selecionada.
     */
    private boolean validateSourceBranch(String sourceBranch) {
        if (sourceBranch == null) {
            Messages.showErrorDialog(project, MessageBundle.message("error.no.source"),
                    MessageBundle.message("dialog.title"));
            return false;
        }
        return true;
    }

    /**
     * Valida as branches target selecionadas.
     */
    private boolean validateTargetBranches(String sourceBranch, List<String> targetBranches) {
        if (targetBranches.isEmpty()) {
            Messages.showErrorDialog(project, MessageBundle.message("error.no.targets"),
                    MessageBundle.message("dialog.title"));
            return false;
        }
        if (targetBranches.contains(sourceBranch)) {
            Messages.showErrorDialog(project,
                    MessageBundle.message("error.branch.protected", sourceBranch),
                    MessageBundle.message("dialog.title"));
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