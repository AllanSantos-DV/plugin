package com.plugin.gitmultimerge.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.plugin.gitmultimerge.util.MessageBundle;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Painel customizado para seleção da branch source, incluindo ComboBox, busca e
 * aviso.
 */
public class SourceBranchPanel extends JPanel {
    private final ComboBox<String> sourceBranchComboBox;
    private final JBLabel warningLabel;
    private final List<String> allBranchNames;
    private final String currentBranch;

    public SourceBranchPanel(List<String> allBranchNames, String currentBranch, Runnable onBranchChanged) {
        super(new BorderLayout(0, 5));
        this.allBranchNames = allBranchNames;
        this.currentBranch = currentBranch;
        add(new JBLabel(MessageBundle.message("source.branch.label")), BorderLayout.NORTH);

        DefaultComboBoxModel<String> originalModel = new DefaultComboBoxModel<>(allBranchNames.toArray(new String[0]));
        sourceBranchComboBox = new ComboBox<>(originalModel);
        setBranch(currentBranch);
        installSpeedSearch();
        addKeyListener();
        addPopupListener();
        sourceBranchComboBox.addActionListener(e -> onBranchChanged.run());

        JPanel comboPanel = new JPanel(new BorderLayout());
        comboPanel.add(sourceBranchComboBox, BorderLayout.CENTER);
        add(comboPanel, BorderLayout.CENTER);

        warningLabel = new JBLabel();
        warningLabel.setForeground(JBColor.RED);
        warningLabel.setVisible(false);
        add(warningLabel, BorderLayout.SOUTH);
    }

    public void setBranch(String branch) {
        if (branch != null && allBranchNames.contains(branch)) {
            sourceBranchComboBox.setSelectedItem(branch);
        } else {
            sourceBranchComboBox.setSelectedItem(currentBranch);
        }
    }

    public String getSelectedBranch() {
        Object selectedBranch = sourceBranchComboBox.getSelectedItem();
        if (selectedBranch == null || selectedBranch.toString().isEmpty()) {
            return currentBranch;
        }
        return selectedBranch.toString();
    }

    public void setWarning(String message) {
        warningLabel.setText(message);
        warningLabel.setVisible(true);
    }

    public boolean hasWarning() {
        return warningLabel != null && warningLabel.isVisible();
    }

    public void clearWarning() {
        warningLabel.setVisible(false);
    }

    private void installSpeedSearch() {
        ComboboxSpeedSearch.installSpeedSearch(sourceBranchComboBox, s -> {
            if (!sourceBranchComboBox.isPopupVisible()) {
                sourceBranchComboBox.showPopup();
            }
            return s;
        });
    }

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

    private void addPopupListener() {
        sourceBranchComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String branch = sourceBranchComboBox.getEditor().getItem().toString();
                    DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) sourceBranchComboBox.getModel();
                    model.removeAllElements();
                    model.addAll(allBranchNames);
                    setBranch(branch);
                });
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }
}