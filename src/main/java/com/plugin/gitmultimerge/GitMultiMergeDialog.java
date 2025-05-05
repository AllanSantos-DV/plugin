package com.plugin.gitmultimerge;

import com.intellij.openapi.application.ApplicationManager;
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
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
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
    private final Git git;

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
        this.git = Git.getInstance();

        setTitle(MessageBundle.message("dialog.title"));
        init();

        // Verifica se a branch current tem alterações não commitadas
        checkSourceBranchUncommittedChangesAsync();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // Configuração do painel principal
        c.fill = GridBagConstraints.BOTH;
        c.insets = JBUI.insets(5);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0; // Ocupar toda a largura horizontal

        // ComboBox de branch source
        JPanel sourcePanel = new JPanel(new BorderLayout(0, 5));
        sourcePanel.add(new JBLabel(MessageBundle.message("source.branch.label")), BorderLayout.NORTH);

        sourceBranchComboBox = new ComboBox<>(allBranchNames.toArray(new String[0]));
        // Tentar selecionar a branch atual
        String currentBranch = repository.getCurrentBranchName();
        if (currentBranch != null) {
            sourceBranchComboBox.setSelectedItem(currentBranch);
        }
        sourcePanel.add(sourceBranchComboBox, BorderLayout.CENTER);

        // Adiciona o label de aviso de alterações não commitadas
        warningLabel = new JBLabel();
        warningLabel.setForeground(JBColor.RED);
        warningLabel.setVisible(false);
        sourcePanel.add(warningLabel, BorderLayout.SOUTH);

        c.weighty = 0;
        panel.add(sourcePanel, c);

        // Lista de branches target com campo de busca
        c.gridy = 1;
        c.weighty = 1.0; // Dar mais espaço vertical para a lista de targets (aumentado)

        JPanel targetMainPanel = new JPanel(new BorderLayout(0, 5));
        targetMainPanel.add(new JBLabel(MessageBundle.message("target.branches.label")), BorderLayout.NORTH);

        // Painel para a lista de targets com campo de busca
        JPanel targetPanel = new JPanel(new BorderLayout(0, 5));

        // Campo de busca
        searchField = new JBTextField();
        searchField.getEmptyText().setText(MessageBundle.message("target.branches.search"));
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
        sourceBranchComboBox.addActionListener(e -> {
            updateTargetList();
            checkSourceBranchUncommittedChangesAsync();
        });

        // Limite a seleção para no máximo 5 branches target
        targetBranchList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && targetBranchList.getSelectedIndices().length > 5) {
                targetBranchList.setSelectedIndices(
                        java.util.Arrays.copyOf(targetBranchList.getSelectedIndices(), 5));
            }
        });

        JBScrollPane targetScrollPane = new JBScrollPane(targetBranchList);
        targetPanel.add(targetScrollPane, BorderLayout.CENTER);
        targetMainPanel.add(targetPanel, BorderLayout.CENTER);

        panel.add(targetMainPanel, c);

        // Opções adicionais - Agora mais compactas
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints optConstraints = new GridBagConstraints();
        optConstraints.fill = GridBagConstraints.HORIZONTAL;
        optConstraints.weightx = 1.0;
        optConstraints.gridx = 0;
        optConstraints.insets = JBUI.insets(2); // Espaçamento reduzido

        // Checkboxes mais compactos
        squashCheckBox = new JBCheckBox(MessageBundle.message("options.squash.commits"));
        optConstraints.gridy = 0;
        optionsPanel.add(squashCheckBox, optConstraints);

        deleteSourceCheckBox = new JBCheckBox(MessageBundle.message("options.delete.branch.after"));
        optConstraints.gridy = 1;
        optionsPanel.add(deleteSourceCheckBox, optConstraints);

        pushAfterMergeCheckBox = new JBCheckBox(MessageBundle.message("options.push.after.merge"));
        pushAfterMergeCheckBox.setSelected(true); // Habilitado por padrão
        optConstraints.gridy = 2;
        optionsPanel.add(pushAfterMergeCheckBox, optConstraints);

        // Painel para mensagem de commit
        JPanel commitMessagePanel = new JPanel(new BorderLayout(5, 2));
        commitMessagePanel.add(new JBLabel(MessageBundle.message("options.commit.message")), BorderLayout.NORTH);
        mergeCommitMessageField = new JBTextField();
        mergeCommitMessageField.setEnabled(true);
        commitMessagePanel.add(mergeCommitMessageField, BorderLayout.CENTER);

        optConstraints.gridy = 3;
        optConstraints.insets = JBUI.insets(5, 2, 2, 2); // Adiciona um pouco mais de espaço acima
        optionsPanel.add(commitMessagePanel, optConstraints);

        c.gridy = 2;
        c.weighty = 0;
        panel.add(optionsPanel, c);

        // Tamanho do diálogo - agora mais estreito e mais alto
        panel.setPreferredSize(new Dimension(450, 550));

        return panel;
    }

    /**
     * Verifica se a branch source selecionada tem alterações não commitadas de forma assíncrona.
     * Esta abordagem evita bloquear a EDT.
     */
    private void checkSourceBranchUncommittedChangesAsync() {
        String selectedBranch = (String) sourceBranchComboBox.getSelectedItem();
        if (selectedBranch == null)
            return;

        // Desabilita o botão OK temporariamente até a verificação terminar
        setOKActionEnabled(false);
        warningLabel.setVisible(false);

        // Verifica se a branch selecionada é a atual
        String currentBranch = repository.getCurrentBranchName();
        if (currentBranch != null && currentBranch.equals(selectedBranch)) {
            // Executa a verificação em uma thread em background
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                boolean hasChanges = hasUncommittedChangesNonEDT();

                // Volta para a EDT para atualizar a UI
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (hasChanges) {
                        // Mostra mensagem de aviso
                        warningLabel.setText(MessageBundle.message("error.source.uncommitted.changes.message"));
                        warningLabel.setVisible(true);
                        setOKActionEnabled(false);
                    } else {
                        // Esconde a mensagem de aviso
                        warningLabel.setVisible(false);
                        setOKActionEnabled(true);
                    }
                });
            });
        } else {
            // Se não for a branch atual, habilita o botão OK
            setOKActionEnabled(true);
        }
    }

    /**
     * Verifica se há alterações não commitadas no working directory.
     * Esta função deve ser chamada apenas de uma thread background.
     *
     * @return true se existem alterações não commitadas, false se o working
     *         directory está limpo
     */
    private boolean hasUncommittedChangesNonEDT() {
        GitLineHandler statusHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.STATUS);
        statusHandler.addParameters("--porcelain");
        GitCommandResult statusResult = git.runCommand(statusHandler);

        // Se a saída não estiver vazia, significa que há alterações não commitadas
        return statusResult.getOutput().stream().anyMatch(line -> !line.trim().isEmpty());
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
        // Validação de entrada
        String sourceBranch = (String) sourceBranchComboBox.getSelectedItem();
        List<String> targetBranches = targetBranchList.getSelectedValuesList();

        if (sourceBranch == null) {
            Messages.showErrorDialog(project, MessageBundle.message("error.no.source"),
                    MessageBundle.message("dialog.title"));
            return;
        }

        // Verificação final antes de prosseguir
        String currentBranch = repository.getCurrentBranchName();
        if (currentBranch != null && currentBranch.equals(sourceBranch)) {
            // Executa a verificação em uma task modal para feedback visual
            ProgressManager.getInstance().run(new Task.Modal(project, MessageBundle.message("progress.checking.changes"), true) {
                private boolean hasChanges = false;

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    hasChanges = hasUncommittedChangesNonEDT();
                }

                @Override
                public void onSuccess() {
                    if (hasChanges) {
                        Messages.showErrorDialog(
                                project,
                                MessageBundle.message("error.source.uncommitted.changes.details", sourceBranch),
                                MessageBundle.message("error.source.uncommitted.changes"));
                    } else {
                        // Só prossegue se não houver alterações
                        continueWithOkAction(sourceBranch, targetBranches);
                    }
                }
            });
        } else {
            // Se não for a branch atual, prossegue normalmente
            continueWithOkAction(sourceBranch, targetBranches);
        }
    }

    /**
     * Continua com a ação OK após validações
     */
    private void continueWithOkAction(String sourceBranch, List<String> targetBranches) {
        if (targetBranches.isEmpty()) {
            Messages.showErrorDialog(project, MessageBundle.message("error.no.targets"),
                    MessageBundle.message("dialog.title"));
            return;
        }

        if (targetBranches.contains(sourceBranch)) {
            Messages.showErrorDialog(project,
                    MessageBundle.message("error.branch.protected", sourceBranch),
                    MessageBundle.message("dialog.title"));
            return;
        }

        // Configurações adicionais
        boolean squash = squashCheckBox.isSelected();
        boolean deleteSource = deleteSourceCheckBox.isSelected();
        boolean pushAfterMerge = pushAfterMergeCheckBox.isSelected();
        String mergeMessage = mergeCommitMessageField.getText();

        // Inicia o processo de merge em background
        ProgressManager.getInstance()
                .run(new Task.Backgroundable(project, MessageBundle.message("dialog.title"), true) {
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
                            NotificationHelper.notifyError(project, NotificationHelper.DEFAULT_TITLE, throwable);
                            return false;
                        });
                    }
                });

        super.doOKAction();
    }
}