package com.plugin.gitmultimerge.ui;

import com.intellij.icons.AllIcons;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Renderer customizado para exibir ícones de pasta e branch na árvore de
 * branches.
 */
public class BranchTreeCellRenderer extends DefaultTreeCellRenderer {
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