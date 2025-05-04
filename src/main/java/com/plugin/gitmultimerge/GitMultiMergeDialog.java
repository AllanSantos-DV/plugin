package com.plugin.gitmultimerge;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.plugin.gitmultimerge.service.GitMultiMergeService;
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

    public GitMultiMergeDialog(@NotNull Project project, @NotNull GitRepository repository) {
        super(project);
        this.project = project;
        this.repository = repository;
        // Obtém o serviço Git Multi Merge do registro de serviços do projeto
        this.gitService = project.getService(GitMultiMergeService.class);
        // Usa o serviço para obter os nomes das branches
        this.allBranchNames = gitService.getBranchNames(repository);

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

        // ComboBox de branch source
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.weighty = 0;
        panel.add(new JBLabel("Branch source:"), c);

        c.gridy = 1;
        c.weighty = 0.1;
        sourceBranchComboBox = new ComboBox<>(allBranchNames.toArray(new String[0]));

        // Tentar selecionar a branch atual
        String currentBranch = repository.getCurrentBranchName();
        if (currentBranch != null) {
            sourceBranchComboBox.setSelectedItem(currentBranch);
        }

        panel.add(sourceBranchComboBox, c);

        // Lista de branches target com campo de busca
        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 0;
        panel.add(new JBLabel("Branches Target (máx. 5):"), c);

        c.gridy = 1;
        c.weighty = 1.0;

        // Painel para a lista de targets com campo de busca
        JPanel targetPanel = new JPanel(new BorderLayout(0, 5));

        // Campo de busca
        searchField = new JBTextField();
        searchField.getEmptyText().setText("Buscar branches...");
        targetPanel.add(searchField, BorderLayout.NORTH);

        // Lista de branches target
        targetListModel = new DefaultListModel<>();
        targetBranchList = new JBList<>(targetListModel);
        targetBranchList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Atualizar inicialmente a lista de targets
        updateTargetList();

        // Adicionar listener para o campo de busca
        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateTargetList();
            }
        });

        // Adicionar listener para o combobox de source
        sourceBranchComboBox.addActionListener(e -> updateTargetList());

        // Limite a seleção para no máximo 5 branches target
        targetBranchList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && targetBranchList.getSelectedIndices().length > 5) {
                targetBranchList.setSelectedIndices(
                        java.util.Arrays.copyOf(targetBranchList.getSelectedIndices(), 5));
            }
        });

        JBScrollPane targetScrollPane = new JBScrollPane(targetBranchList);
        targetPanel.add(targetScrollPane, BorderLayout.CENTER);
        panel.add(targetPanel, c);

        // Opções adicionais
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));

        squashCheckBox = new JBCheckBox("Squash commits");
        deleteSourceCheckBox = new JBCheckBox("Deletar branch source após o merge");
        pushAfterMergeCheckBox = new JBCheckBox("Push para remote após o merge");
        pushAfterMergeCheckBox.setSelected(true); // Habilitado por padrão

        // Painel para mensagem de commit
        JPanel commitMessagePanel = new JPanel(new BorderLayout(5, 5));
        commitMessagePanel.add(new JBLabel("Mensagem de merge (opcional):"), BorderLayout.NORTH);
        mergeCommitMessageField = new JBTextField();
        mergeCommitMessageField.setEnabled(true);
        commitMessagePanel.add(mergeCommitMessageField, BorderLayout.CENTER);

        optionsPanel.add(squashCheckBox);
        optionsPanel.add(deleteSourceCheckBox);
        optionsPanel.add(pushAfterMergeCheckBox);
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

    @Override
    protected void doOKAction() {
        // Validação de entrada
        String sourceBranch = (String) sourceBranchComboBox.getSelectedItem();
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

        // Configurações adicionais
        boolean squash = squashCheckBox.isSelected();
        boolean deleteSource = deleteSourceCheckBox.isSelected();
        boolean pushAfterMerge = pushAfterMergeCheckBox.isSelected();
        String mergeMessage = mergeCommitMessageField.getText();

        // Inicia o processo de merge em background com barra de progresso
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Git multi merge", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // Usa o serviço GitMultiMergeService para executar o merge
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
                    // Tratamento de erro simplificado
                    NotificationHelper.notifyError(project, NotificationHelper.DEFAULT_TITLE, throwable);
                    return false;
                });
            }
        });

        super.doOKAction();
    }
}