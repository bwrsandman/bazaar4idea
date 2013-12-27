// Copyright 2009 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package bazaar4idea.ui;

import bazaar4idea.BzrUtil;
import bazaar4idea.command.BzrCommand;
import bazaar4idea.command.BzrLineHandler;
import bazaar4idea.i18n.BzrBundle;
import bazaar4idea.merge.BzrMergeUtil;
import bazaar4idea.repo.BzrRemote;
import bazaar4idea.repo.BzrRepositoryManager;
import bazaar4idea.util.BzrUIUtil;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang.StringUtils;
import bazaar4idea.command.BzrShowConfigCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

public class BzrPullDialog extends DialogWrapper {

  private static final Logger LOG = Logger.getInstance(BzrPullDialog.class);

  private final Project myProject;
  private final BzrRepositoryManager myRepositoryManager;

  private BzrRepositorySelectorComponent bzrRepositorySelector;

  private JTextField sourceTxt;

  private JPanel mainPanel;
  /**
   * The selected Bazaar root
   */
  private JComboBox myBzrRoot;
  /**
   * Current branch label
   */
  private JLabel myCurrentBranch;
  /**
   * The merge strategy
   */
  private JComboBox myStrategy;
  /**
   * No commit option
   */
  private JCheckBox myNoCommitCheckBox;
  /**
   * Squash commit option
   */
  private JCheckBox mySquashCommitCheckBox;
  /**
   * No fast forward option
   */
  private JCheckBox myNoFastForwardCheckBox;
  /**
   * Add log info to commit option
   */
  private JCheckBox myAddLogInformationCheckBox;
  /**
   * Selected remote option
   */
  private JComboBox myRemote;
  /**
   * The branch chooser
   */
  private ElementsChooser<String> myBranchChooser;

  public BzrPullDialog(Project project, List<VirtualFile> roots, VirtualFile defaultRoot) {
    super(project, true);
    setTitle(BzrBundle.getString("pull.title"));
    myProject = project;
    myRepositoryManager = BzrUtil.getRepositoryManager(myProject);
    BzrUIUtil.setupRootChooser(myProject, roots, defaultRoot, myBzrRoot, myCurrentBranch);
    myBzrRoot.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        // TODO
//        updateRemotes();
      }
    });
    setOKButtonText(BzrBundle.getString("pull.button"));
    // TODO
//    updateRemotes();
//    updateBranches();
    myRemote.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO
//        updateBranches();
      }
    });
    bzrRepositorySelector.setTitle("Select repository to pull changesets for");
    bzrRepositorySelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onChangeRepository();
      }
    });
    DocumentListener documentListener = new DocumentListener() {
      public void insertUpdate(DocumentEvent e) {
        onChangePullSource();
      }

      public void removeUpdate(DocumentEvent e) {
        onChangePullSource();
      }

      public void changedUpdate(DocumentEvent e) {
        onChangePullSource();
      }
    };
    sourceTxt.getDocument().addDocumentListener(documentListener);
    init();
  }

  public VirtualFile getRepository() {
    return bzrRepositorySelector.getRepository();
  }

  public String getSource() {
    return sourceTxt.getText();
  }

  public void setRoots(Collection<VirtualFile> repos) {
    bzrRepositorySelector.setRoots(repos);
    onChangeRepository();
  }

  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  private void onChangeRepository() {
    VirtualFile repo = bzrRepositorySelector.getRepository();
    BzrShowConfigCommand configCommand = new BzrShowConfigCommand(myProject);
    String defaultPath = configCommand.getDefaultPath(repo);
    sourceTxt.setText(defaultPath);
    onChangePullSource();
  }

  private void onChangePullSource() {
    setOKActionEnabled(StringUtils.isNotBlank(sourceTxt.getText()));
  }

  @Nullable
  public String getRemote() {
    BzrRemote remote = (BzrRemote)myRemote.getSelectedItem();
    return remote == null ? null : remote.getName();
  }

  /**
   * @return a currently selected Bazaar root
   */
  public VirtualFile bzrRoot() {
    return (VirtualFile)myBzrRoot.getSelectedItem();
  }

  /**
   * @return a pull handler configured according to dialog options
   */
  public BzrLineHandler makeHandler(@NotNull String url) {
    BzrLineHandler h = new BzrLineHandler(myProject, bzrRoot(), BzrCommand.PULL);
    // ignore merge failure for the pull
    h.ignoreErrorCode(1);
    h.setUrl(url);
    //TODO
//    h.addProgressParameter();
    h.addParameters("--no-stat");
    if (myNoCommitCheckBox.isSelected()) {
      h.addParameters("--no-commit");
    }
    else {
      if (myAddLogInformationCheckBox.isSelected()) {
        h.addParameters("--log");
      }
    }
    if (mySquashCommitCheckBox.isSelected()) {
      h.addParameters("--squash");
    }
    if (myNoFastForwardCheckBox.isSelected()) {
      h.addParameters("--no-ff");
    }
    String strategy = (String)myStrategy.getSelectedItem();
    if (!BzrMergeUtil.DEFAULT_STRATEGY.equals(strategy)) {
      h.addParameters("--strategy", strategy);
    }
    h.addParameters("-v");
    //TODO
//    h.addProgressParameter();

    final List<String> markedBranches = myBranchChooser.getMarkedElements();
    String remote = getRemote();
    LOG.assertTrue(remote != null, "Selected remote can't be null here.");
    // git pull origin master (remote branch name in the format local to that remote)
    h.addParameters(remote);
//    for (String branch : markedBranches) {
//      h.addParameters(removeRemotePrefix(branch, remote));
//    }
    return h;
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
    mainPanel = new JPanel();
    mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
    bzrRepositorySelector = new BzrRepositorySelectorComponent();
    mainPanel.add(bzrRepositorySelector.$$$getRootComponent$$$(),
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
            0, false));
    final Spacer spacer1 = new Spacer();
    mainPanel.add(spacer1,
        new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
    mainPanel.add(panel1,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
            0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Pull From:");
    panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel1.add(spacer2,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    sourceTxt = new JTextField();
    panel1.add(sourceTxt,
        new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() {
    return mainPanel;
  }
}
