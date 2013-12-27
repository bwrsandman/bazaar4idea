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
import bazaar4idea.command.BzrHandlerUtil;
import bazaar4idea.command.BzrSimpleHandler;
import bazaar4idea.i18n.BzrBundle;
import bazaar4idea.repo.BzrRepositoryManager;
import bazaar4idea.util.BzrUIUtil;
import bazaar4idea.util.StringScanner;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BzrTagDialog extends DialogWrapper {

  private JPanel myPanel;

  /**
   * Bazaar root selector
   */
  private JComboBox myBzrRootComboBox;
  /**
   * Current branch label
   */
  private JLabel myCurrentBranch;
  /**
   * Tag name
   */
  private JTextField myTagNameTextField;
  /**
   * Force tag creation checkbox
   */
  private JCheckBox myForceCheckBox;
  /**
   * Text area that contains tag message if non-empty
   */
  private JTextArea myMessageTextArea;
  /**
   * The name of commit to tag
   */
  private JTextField myCommitTextField;
  /**
   * The validate button
   */
  private JButton myValidateButton;
  /**
   * The validator for commit text field
   */
  private final BzrReferenceValidator myCommitTextFieldValidator;


  private BzrRepositorySelectorComponent hgRepositorySelectorComponent;

  /**
   * The current project
   */
  private final Project myProject;
  /**
   * Existing tags for the project
   */
  private final Set<String> myExistingTags = new HashSet<String>();

  /**
   * Prefix for message file name
   */
  @NonNls private static final String MESSAGE_FILE_PREFIX = "bzr-tag-message-";
  /**
   * Suffix for message file name
   */
  @NonNls private static final String MESSAGE_FILE_SUFFIX = ".txt";
  /**
   * Encoding for the message file
   */
  @NonNls private static final String MESSAGE_FILE_ENCODING = "UTF-8";


  /**
   * A constructor
   *
   * @param project     a project to select
   * @param roots       a Bazaar repository roots for the project
   * @param defaultRoot a guessed default root
   */
  public BzrTagDialog(Project project, List<VirtualFile> roots, VirtualFile defaultRoot) {
    super(project, false);
    setTitle(BzrBundle.getString("tag.title"));
    setOKButtonText(BzrBundle.getString("tag.button"));
    myProject = project;
    BzrUIUtil.setupRootChooser(myProject, roots, defaultRoot, myBzrRootComboBox, myCurrentBranch);
    myBzrRootComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        fetchTags();
        validateFields();
      }
    });
    fetchTags();
    myTagNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        validateFields();
      }
    });
    myCommitTextFieldValidator = new BzrReferenceValidator(project, myBzrRootComboBox, myCommitTextField, myValidateButton, new Runnable() {
      public void run() {
        validateFields();
      }
    });
    myForceCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (myForceCheckBox.isEnabled()) {
          validateFields();
        }
      }
    });
    init();
    validateFields();
  }

  public String getTagName() {
    return myTagNameTextField.getText();
  }

  public VirtualFile getRepository() {
    return hgRepositorySelectorComponent.getRepository();
  }

  public void setRoots(Collection<VirtualFile> repos) {
    hgRepositorySelectorComponent.setRoots(repos);
    update();
  }

  protected JComponent createCenterPanel() {
    return myPanel;
  }

  private void update() {
    setOKActionEnabled(validateOptions());
  }

  private boolean validateOptions() {
    return StringUtils.isNotBlank(myTagNameTextField.getText());
  }

  /**
   * Validate dialog fields
   */
  private void validateFields() {
    String text = myTagNameTextField.getText();
    if (myExistingTags.contains(text)) {
      myForceCheckBox.setEnabled(true);
      if (!myForceCheckBox.isSelected()) {
        setErrorText(BzrBundle.getString("tag.error.tag.exists"));
        setOKActionEnabled(false);
        return;
      }
    }
    else {
      myForceCheckBox.setEnabled(false);
      myForceCheckBox.setSelected(false);
    }
    if (myCommitTextFieldValidator.isInvalid()) {
      setErrorText(BzrBundle.getString("tag.error.invalid.commit"));
      setOKActionEnabled(false);
      return;
    }
    if (text.length() == 0) {
      setErrorText(null);
      setOKActionEnabled(false);
      return;
    }
    setErrorText(null);
    setOKActionEnabled(true);
  }

  /**
   * Fetch tags
   */
  private void fetchTags() {
    myExistingTags.clear();
    BzrSimpleHandler h = new BzrSimpleHandler(myProject, getBzrRoot(), BzrCommand.TAG);
    h.setSilent(true);
    String output = BzrHandlerUtil.doSynchronously(h, BzrBundle.getString("tag.getting.existing.tags"), h.printableCommandLine());
    for (StringScanner s = new StringScanner(output); s.hasMoreData();) {
      String line = s.line();
      if (line.length() == 0) {
        continue;
      }
      myExistingTags.add(line);
    }
  }

  /**
   * @return the current Bazaar root
   */
  private VirtualFile getBzrRoot() {
    return (VirtualFile)myBzrRootComboBox.getSelectedItem();
  }

  /**
   * Perform tagging according to selected options
   *
   * @param exceptions the list where exceptions are collected
   */
  public void runAction(final List<VcsException> exceptions) {
    final String message = myMessageTextArea.getText();
    final boolean hasMessage = message.trim().length() != 0;
    final File messageFile;
    if (hasMessage) {
      try {
        messageFile = FileUtil.createTempFile(MESSAGE_FILE_PREFIX, MESSAGE_FILE_SUFFIX);
        messageFile.deleteOnExit();
        Writer out = new OutputStreamWriter(new FileOutputStream(messageFile), MESSAGE_FILE_ENCODING);
        try {
          out.write(message);
        }
        finally {
          out.close();
        }
      }
      catch (IOException ex) {
        Messages.showErrorDialog(myProject, BzrBundle.message("tag.error.creating.message.file.message", ex.toString()),
                BzrBundle.getString("tag.error.creating.message.file.title"));
        return;
      }
    }
    else {
      messageFile = null;
    }
    try {
      // TODO verify invocation in Bazaar vs. git
      BzrSimpleHandler h = new BzrSimpleHandler(myProject, getBzrRoot(), BzrCommand.TAG);
      if (hasMessage) {
        h.addParameters("-a");
      }
      if (myForceCheckBox.isEnabled() && myForceCheckBox.isSelected()) {
        h.addParameters("-f");
      }
      if (hasMessage) {
        h.addParameters("-F", messageFile.getAbsolutePath());
      }
      h.addParameters(myTagNameTextField.getText());
      String object = myCommitTextField.getText().trim();
      if (object.length() != 0) {
        h.addParameters(object);
      }
      try {
        BzrHandlerUtil.doSynchronously(h, BzrBundle.getString("tagging.title"), h.printableCommandLine());
        BzrUIUtil.notifySuccess(myProject, myTagNameTextField.getText(), "Created tag "  + myTagNameTextField.getText() + " successfully.");
      }
      finally {
        exceptions.addAll(h.errors());
        BzrRepositoryManager manager = BzrUtil.getRepositoryManager(myProject);
        manager.updateRepository(getBzrRoot());
      }
    }
    finally {
      if (messageFile != null) {
        //noinspection ResultOfMethodCallIgnored
        messageFile.delete();
      }
    }
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
    myPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    final Spacer spacer1 = new Spacer();
    myPanel.add(spacer1,
            new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                    GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    hgRepositorySelectorComponent = new BzrRepositorySelectorComponent();
    myPanel.add(hgRepositorySelectorComponent.$$$getRootComponent$$$(),
            new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                    0, false));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
    myPanel.add(panel1,
            new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                    0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Tag name:");
    panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel1.add(spacer2,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    myTagNameTextField = new JTextField();
    panel1.add(myTagNameTextField,
        new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(150, -1), null, 0, false));
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() {
    return myPanel;
  }
}
