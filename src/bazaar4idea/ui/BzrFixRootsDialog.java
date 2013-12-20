/*
 * Copyright (c) 2010 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package bazaar4idea.ui;

import bazaar4idea.i18n.BzrBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.TreeSet;

/**
 * This dialog shows a new git root set
 */
class BzrFixRootsDialog extends DialogWrapper {

  /**
   * The list of roots
   */
  private JList myGitRoots;
  /**
   * The root panel
   */
  private JPanel myPanel;

  /**
   * The constructor
   *
   * @param project the context project
   */
  protected BzrFixRootsDialog(
      Project project, HashSet<String> current, HashSet<String> added, HashSet<String> removed) {
    super(project, true);
    setTitle(BzrBundle.message("fix.roots.title"));
    setOKButtonText(BzrBundle.message("fix.roots.button"));
    TreeSet<Item> items = new TreeSet<Item>();
    for (String f : added) {
      items.add(new Item(f, FileStatus.ADDED));
    }
    for (String f : current) {
      items.add(new Item(f, removed.contains(f) ? FileStatus.DELETED : FileStatus.NOT_CHANGED));
    }
    DefaultListModel listModel = new DefaultListModel();
    for (Item i : items) {
      listModel.addElement(i);
    }
    myGitRoots.setModel(listModel);
    init();
  }

  /**
   * {@inheritDoc}
   */
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getDimensionServiceKey() {
    return getClass().getName();
  }

  {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
  }

  /** Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    myPanel = new JPanel();
    myPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
    final JScrollPane scrollPane1 = new JScrollPane();
    myPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    myGitRoots = new JList();
    myGitRoots.setToolTipText(
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages").getString("fix.roots.list.tooltip"));
    scrollPane1.setViewportView(myGitRoots);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    myPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1,
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages").getString("fix.roots.message"));
    label1.putClientProperty("html.disable", Boolean.FALSE);
    panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    label1.setLabelFor(scrollPane1);
  }

  /** @noinspection ALL */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() {
    return myPanel;
  }

  /**
   * The item in the list
   */
  private class Item implements Comparable<Item> {

    /**
     * The status of the file
     */
    @NotNull
    final FileStatus status;
    /**
     * The file name
     */
    @NotNull
    final String fileName;

    /**
     * The constructor
     *
     * @param fileName the root path
     * @param status   the root status
     */
    public Item(@NotNull String fileName, @NotNull FileStatus status) {
      this.fileName = fileName;
      this.status = status;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Item o) {
      return fileName.compareTo(o.fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      if (status == FileStatus.ADDED) {
        return "<html><b>" + fileName + "</b></html>";
      } else if (status == FileStatus.DELETED) {
        return "<html><strike>" + fileName + "</strike></html>";
      } else {
        return fileName;
      }
    }
  }
}
