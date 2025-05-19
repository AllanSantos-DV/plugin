package com.plugin.gitmultimerge.ui;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.plugin.gitmultimerge.util.MessageBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Painel customizado para exibir e gerir a árvore de branches target com
 * campo de busca.
 */
public class BranchTreePanel extends JPanel {
    private final JBTextField searchField;
    private final JTree branchTree;
    private final DefaultMutableTreeNode treeRoot;
    private final List<String> allBranchNames;
    private String sourceBranch;

    public BranchTreePanel(List<String> allBranchNames, String sourceBranch) {
        super(new BorderLayout(0, 5));
        this.allBranchNames = allBranchNames;
        this.sourceBranch = sourceBranch;
        this.treeRoot = new DefaultMutableTreeNode("branches");
        this.branchTree = new Tree(treeRoot);
        branchTree.setSelectionModel(new DefaultTreeSelectionModel());
        branchTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        branchTree.setRootVisible(false);
        branchTree.setCellRenderer(new BranchTreeCellRenderer());

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JBTextField();
        searchField.getEmptyText().setText(MessageBundle.message("search.branch.placeholder"));
        searchPanel.add(searchField, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);
        add(new JBScrollPane(branchTree), BorderLayout.CENTER);

        updateTree();
        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateTree();
            }
        });
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
        updateTree();
    }

    public List<String> getSelectedBranches() {
        TreePath[] paths = branchTree.getSelectionPaths();
        List<String> selected = new ArrayList<>();
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
            branchTree.setSelectionPaths(leafPaths.toArray(new TreePath[0]));
            selected = getSelectedTargetBranches(paths);

        }
        return selected;
    }

    private List<String> getSelectedTargetBranches(@NotNull TreePath[] paths) {
        List<String> selected = new java.util.ArrayList<>();
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
        return selected;
    }

    public void addTreeSelectionListener(TreeSelectionListener listener) {
        branchTree.addTreeSelectionListener(listener);
    }

    private void updateTree() {
        String searchText = searchField.getText();
        treeRoot.removeAllChildren();
        for (String branch : allBranchNames) {
            if (branch.equals(sourceBranch)) continue;
            if (searchText == null || searchText.isEmpty()
                    || branch.toLowerCase().contains(searchText.toLowerCase())) {
                addBranchToTree(treeRoot, branch);
            }
        }
        ((DefaultTreeModel) branchTree.getModel()).reload();
        for (int i = 0; i < branchTree.getRowCount(); i++) {
            branchTree.collapseRow(i);
        }
        if (searchText != null && !searchText.isEmpty())
            expandAllWithLeaves(branchTree, new TreePath(treeRoot));
    }

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
                reordenaNos(node);
            }
            node = child;
        }
    }

    private void reordenaNos(DefaultMutableTreeNode pai) {
        int count = pai.getChildCount();
        if (count <= 1)
            return;
        List<DefaultMutableTreeNode> pastas = new ArrayList<>();
        List<DefaultMutableTreeNode> folhas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode filho = (DefaultMutableTreeNode) pai.getChildAt(i);
            if (filho.getChildCount() > 0) {
                pastas.add(filho);
            } else {
                folhas.add(filho);
            }
        }
        Comparator<DefaultMutableTreeNode> comparador = Comparator.comparing(n -> n.getUserObject().toString());
        pastas.sort(comparador);
        folhas.sort(comparador);
        pai.removeAllChildren();
        for (DefaultMutableTreeNode pasta : pastas) {
            pai.add(pasta);
        }
        for (DefaultMutableTreeNode folha : folhas) {
            pai.add(folha);
        }
    }

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
        if (hasLeaf && parent.getPathCount() > 1) {
            tree.expandPath(parent);
        }
    }
}