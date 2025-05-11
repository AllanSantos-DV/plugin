package com.plugin.gitmultimerge.ui;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.plugin.gitmultimerge.service.interfaces.GitMultiMergeService;

import com.plugin.gitmultimerge.util.MessageBundle;
import com.plugin.gitmultimerge.util.NotificationHelper;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import com.intellij.icons.AllIcons;

/**
 * Diálogo para selecionar branches e configurar opções para o multi-merge.
 * Usa GitMultiMergeService para implementar operações Git.
 */
public class GitMultiMergeDialog extends DialogWrapper {
    private final Project project;
    private final GitRepository repository;
    private final GitMultiMergeService gitService;

    private ComboBox<String> sourceBranchComboBox;
    private JTree targetBranchTree;
    private DefaultMutableTreeNode targetTreeRoot;
    private JBTextField searchField;
    private JBCheckBox squashCheckBox;
    private JBCheckBox deleteSourceCheckBox;
    private JBCheckBox pushAfterMergeCheckBox;
    private JBTextField mergeCommitMessageField;
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

        // Modelo original com todas as branches
        DefaultComboBoxModel<String> originalModel = new DefaultComboBoxModel<>(allBranchNames.toArray(new String[0]));
        sourceBranchComboBox = new ComboBox<>(originalModel);
        sourceBranchComboBox.setSelectedItem(Objects.requireNonNull(repository.getCurrentBranch()).getName());

        // Instala o SpeedSearch para o ComboBox
        installSpeedSearch();

        // Adiciona o listener de keyTyped para filtrar as branches
        addKeyListener();

        // Adiciona o listener de popup para restaurar o modelo original
        addPopupListener();

        // Listener para lidar com a seleção de uma branch
        sourceBranchComboBox.addActionListener(e -> updateTargetTree());

        // Montagem do painel
        JPanel comboPanel = new JPanel(new BorderLayout());
        comboPanel.add(sourceBranchComboBox, BorderLayout.CENTER);
        sourcePanel.add(comboPanel, BorderLayout.CENTER);

        // Adiciona o label de aviso
        addWarningLabel(sourcePanel);
        return sourcePanel;
    }

    /**
     * Instala o SpeedSearch no ComboBox para permitir busca por branches.
     */
    private void installSpeedSearch() {
        ComboboxSpeedSearch.installSpeedSearch(sourceBranchComboBox, s -> {
            if (!sourceBranchComboBox.isPopupVisible()) {
                sourceBranchComboBox.showPopup();
            }
            return s;
        });
    }

    /**
     * Adiciona um listener de keyTyped para filtrar as branches no ComboBox.
     */
    private void addKeyListener() {
        sourceBranchComboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                SwingUtilities.invokeLater(() -> {
                    SpeedSearchSupply supply = SpeedSearchSupply.getSupply(sourceBranchComboBox);
                    String texto = supply != null ? supply.getEnteredPrefix() : null;
                    DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) sourceBranchComboBox.getModel();
                    if (texto != null && !texto.isEmpty()) {
                        model.removeAllElements();
                        for (String branch : allBranchNames) {
                            if (branch.contains(texto)) {
                                model.addElement(branch);
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Adiciona um listener de popup para restaurar o modelo original do ComboBox
     * quando o popup é fechado.
     */
    private void addPopupListener() {
        sourceBranchComboBox.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // Não faz nada
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(()-> {
                    String branch = sourceBranchComboBox.getEditor().getItem().toString();
                    DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) sourceBranchComboBox.getModel();
                    model.removeAllElements();
                    model.addAll(allBranchNames);
                    sourceBranchComboBox.setSelectedItem(branch);
                });
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // Não faz nada
            }
        });
    }

    /**
     * Adiciona um label de aviso para mostrar mensagens de erro.
     */
    private void addWarningLabel(JPanel sourcePanel) {
        warningLabel = new JBLabel();
        warningLabel.setForeground(JBColor.RED);
        warningLabel.setVisible(false);
        sourcePanel.add(warningLabel, BorderLayout.SOUTH);
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
        searchField.getEmptyText().setText(MessageBundle.message("search.branch.placeholder"));
        searchPanel.add(searchField, BorderLayout.CENTER);
        targetPanel.add(searchPanel, BorderLayout.NORTH);

        targetTreeRoot = new DefaultMutableTreeNode("branches");
        targetBranchTree = new Tree(targetTreeRoot);
        targetBranchTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        targetBranchTree.setRootVisible(false);
        targetBranchTree.setCellRenderer(new BranchTreeCellRenderer());
        updateTargetTree();

        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateTargetTree();
            }
        });

        // Adiciona o listener de seleção para permitir múltiplas seleções
        addTreeSelectionListener();

        JBScrollPane targetScrollPane = new JBScrollPane(targetBranchTree);
        targetPanel.add(targetScrollPane, BorderLayout.CENTER);
        targetMainPanel.add(targetPanel, BorderLayout.CENTER);
        return targetMainPanel;
    }

    /**
     * Adiciona um listener de seleção na árvore de branches target.
     * Permite seleção múltipla apenas em folhas e limita a 5 seleções.
     */
    private void addTreeSelectionListener() {
        targetBranchTree.addTreeSelectionListener(e -> {
            // Permitir seleção múltipla apenas em folhas e limitar a 5
            TreePath[] paths = targetBranchTree.getSelectionPaths();
            if (paths != null) {
                List<TreePath> leafPaths = new ArrayList<>();
                for (TreePath path : paths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.isLeaf()) {
                        leafPaths.add(path);
                    }
                }
                if (leafPaths.size() > 5) {
                    leafPaths = leafPaths.subList(0, 5);
                }
                targetBranchTree.setSelectionPaths(leafPaths.toArray(new TreePath[0]));
            }
            boolean hasSelectedLeaf = paths!= null && paths.length > 0;
            boolean warningExists = warningLabel != null && warningLabel.isVisible();
            setOKActionEnabled(hasSelectedLeaf && !warningExists);
        });
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
                String msg = MessageBundle.message("error.source.uncommitted.changes.message");
                // Garante quebra de linha mesmo se vier \n do arquivo de mensagens
                warningLabel.setText("<html>" + msg.replace("\\n", "<br>") + "</html>");
                warningLabel.setVisible(true);
                setOKActionEnabled(false);
            } else {
                warningLabel.setVisible(false);
            }
        }, SwingUtilities::invokeLater);
    }

    /**
     * Atualiza a árvore de branches target com base na branch source selecionada e
     * texto de busca. Expande apenas os grupos relevantes.
     */
    private void updateTargetTree() {
        String sourceBranch = (String) sourceBranchComboBox.getEditor().getItem();
        String searchText = searchField.getText();
        targetTreeRoot.removeAllChildren();
        for (String branch : allBranchNames) {
            if (!branch.equals(sourceBranch) && (searchText == null || searchText.isEmpty()
                    || branch.toLowerCase().contains(searchText.toLowerCase()))) {
                addBranchToTree(targetTreeRoot, branch);
            }
        }
        ((DefaultTreeModel) targetBranchTree.getModel()).reload();
        // Colapsa todos os nós
        for (int i = 0; i < targetBranchTree.getRowCount(); i++) {
            targetBranchTree.collapseRow(i);
        }

        if (searchText != null && !searchText.isEmpty())
            expandAllWithLeaves(targetBranchTree, new TreePath(targetTreeRoot));
    }

    /**
     * Expande recursivamente apenas os nós que possuem folhas.
     */
    private void expandAllWithLeaves(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        boolean hasLeaf = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            if (child.isLeaf()) {
                hasLeaf = true;
            } else {
                expandAllWithLeaves(tree, path);
                if (child.getChildCount() > 0) {
                    hasLeaf = true;
                }
            }
        }
        if (hasLeaf && parent.getPathCount() > 1) { // não expande root
            tree.expandPath(parent);
        }
    }

    /**
     * Adiciona uma branch ao modelo de árvore, criando nós intermediários conforme
     * necessário.
     */
    private void addBranchToTree(DefaultMutableTreeNode root, String branch) {
        String[] parts = branch.split("/");
        DefaultMutableTreeNode node = root;
        for (String part : parts) {
            DefaultMutableTreeNode child = null;
            for (int j = 0; j < node.getChildCount(); j++) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getChildAt(j);
                if (n.getUserObject().equals(part)) {
                    child = n;
                    break;
                }
            }
            if (child == null) {
                child = new DefaultMutableTreeNode(part);
                node.add(child);
                // Reordena os nós do pai para que as pastas apareçam antes das branches
                reordenaNos(node);
            }
            node = child;
        }
    }

    /**
     * Reordena os nós de um nó pai para que as pastas apareçam antes das branches.
     */
    private void reordenaNos(DefaultMutableTreeNode pai) {
        int count = pai.getChildCount();
        if (count <= 1) return; // Nada para reordenar

        // Cria listas separadas para pastas e folhas
        List<DefaultMutableTreeNode> pastas = new ArrayList<>();
        List<DefaultMutableTreeNode> folhas = new ArrayList<>();

        // Classifica os nós em pastas e folhas
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode filho = (DefaultMutableTreeNode) pai.getChildAt(i);
            if (temOuTeraFilhos(filho)) {
                pastas.add(filho);
            } else {
                folhas.add(filho);
            }
        }

        // Ordena pastas e folhas alfabeticamente
        Comparator<DefaultMutableTreeNode> comparador = Comparator.comparing(n -> n.getUserObject().toString());
        pastas.sort(comparador);
        folhas.sort(comparador);

        // Remove todos os filhos
        pai.removeAllChildren();

        // Adiciona primeiro as pastas, depois as folhas
        for (DefaultMutableTreeNode pasta : pastas) {
            pai.add(pasta);
        }
        for (DefaultMutableTreeNode folha : folhas) {
            pai.add(folha);
        }
    }

    /**
     * Verifica se um nó tem ou terá filhos (é uma pasta).
     * Um nó representa uma pasta se já tem filhos ou se faz parte de um caminho mais longo.
     */
    private boolean temOuTeraFilhos(DefaultMutableTreeNode no) {
        return no.getChildCount() > 0;
    }

    /**
     * Retorna a lista de branches target selecionadas a partir do JTree.
     */
    private List<String> getSelectedTargetBranches() {
        TreePath[] paths = targetBranchTree.getSelectionPaths();
        List<String> selected = new java.util.ArrayList<>();
        if (paths != null) {
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.isLeaf()) {
                    // Monta o nome completo da branch
                    StringBuilder branchName = new StringBuilder();
                    Object[] nodes = path.getPath();
                    for (int i = 1; i < nodes.length; i++) { // ignora root
                        if (!branchName.isEmpty())
                            branchName.append("/");
                        branchName.append(nodes[i].toString());
                    }
                    selected.add(branchName.toString());
                }
            }
        }
        return selected;
    }

    /**
     * Verifica se há alterações não commitadas durante a operação de OK.
     * Esta verificação é feita de forma síncrona pois o usuário já está aguardando.
     */
    @Override
    protected void doOKAction() {
        String sourceBranch = (String) sourceBranchComboBox.getEditor().getItem();
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

    /**
     * Renderer customizado para exibir ícones de pasta e branch.
     */
    private static class BranchTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (leaf) {
                setIcon(AllIcons.Vcs.Branch);
            } else {
                setIcon(AllIcons.Nodes.Folder);
            }
            return c;
        }
    }
}