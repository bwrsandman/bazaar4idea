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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.emergent.bzr4j.core.utils.BzrCoreUtil;
import bazaar4idea.BzrGlobalSettings;
import bazaar4idea.BzrVcsMessages;
import bazaar4idea.command.ShellCommandService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.ResourceBundle;

public class BzrConfigurationIdePanel implements Disposable {

  private static final Logger LOG = Logger.getInstance(BzrConfigurationIdePanel.class.getName());

  private TextFieldWithBrowseButton pathSelector;

  private JPanel basePanel;
  private JButton pathDefaultButton;
  private JCheckBox m_trimAnnotationCheckBox;
  private JTextField m_envBzrEmailTextField;
  private JTextField m_envBzrHomeTextField;
  private JCheckBox m_granularExecLockingCheckBox;
  private JCheckBox m_envBzrEmailCheckBox;
  private JCheckBox m_envBzrHomeCheckBox;
  private JCheckBox m_modalErrorPopupEnabledCheckBox;

  private final BzrGlobalSettings m_globalSettings;

  private final HashMap<String, EnvVarContainer> m_envWidgets = new HashMap<String, EnvVarContainer>();

  private final MergingUpdateQueue myQueue;

  public BzrConfigurationIdePanel(final BzrGlobalSettings globalSettings) {

    myQueue = new MergingUpdateQueue("idepanelqueue", 500, true, basePanel, null, basePanel, true);

    m_globalSettings = globalSettings;
    loadSettings();

    String title = BzrVcsMessages.message("bzr4intellij.configuration.title");
    String description = BzrVcsMessages.message("bzr4intellij.configuration.description");

    pathSelector.addBrowseFolderListener(
        title, description, null,
        new FileChooserDescriptor(true, false, false, false, false, false),
        new TextComponentAccessor<JTextField>() {
          public String getText(JTextField component) {
            return component.getText();
          }

          public void setText(JTextField component, String text) {
            component.setText(text);
          }
        }
    );

    final ActionListener defaultPathListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pathSelector.setText(BzrGlobalSettings.DEFAULT_EXECUTABLE);
      }
    };

    pathDefaultButton.addActionListener(defaultPathListener);

    pathSelector.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        queueValidationHintsUpdate();
      }
    });
  }

  public void dispose() {
    myQueue.dispose();
  }

  public boolean isModified() {
    for (EnvVarContainer con : m_envWidgets.values()) {
      if (con.isModified())
        return true;
    }
    return (!pathSelector.getText().equals(m_globalSettings.getBzrExecutable())
        || !(m_trimAnnotationCheckBox.isSelected() == m_globalSettings.isAnnotationTrimmingEnabled())
        || !(m_modalErrorPopupEnabledCheckBox.isSelected() == m_globalSettings.isModalErrorPopupEnabled())
        || !(m_granularExecLockingCheckBox.isSelected() == m_globalSettings.isAnnotationTrimmingEnabled()));
  }

  public JPanel getBasePanel() {
    return basePanel;
  }

  public void saveSettings() {
    m_globalSettings.setBzrExecutable(pathSelector.getText());
    m_globalSettings.setAnnotationTrimmingEnabled(m_trimAnnotationCheckBox.isSelected());
    m_globalSettings.setModalErrorPopupEnabled(m_modalErrorPopupEnabledCheckBox.isSelected());
    m_globalSettings.setGranularExecLockingEnabled(m_granularExecLockingCheckBox.isSelected());
    for (EnvVarContainer con : m_envWidgets.values()) {
      con.saveEnvVarWidgets();
    }
  }

  public void loadSettings() {
    pathSelector.setText(m_globalSettings.getBzrExecutable());
    m_trimAnnotationCheckBox.setSelected(m_globalSettings.isAnnotationTrimmingEnabled());
    m_modalErrorPopupEnabledCheckBox.setSelected(m_globalSettings.isModalErrorPopupEnabled());
    m_granularExecLockingCheckBox.setSelected(m_globalSettings.isGranularExecLockingEnabled());
    m_envWidgets.put("BZR_EMAIL", new EnvVarContainer("BZR_EMAIL", m_envBzrEmailCheckBox, m_envBzrEmailTextField));
    m_envWidgets.put("BZR_HOME", new EnvVarContainer("BZR_HOME", m_envBzrHomeCheckBox, m_envBzrHomeTextField));
    for (EnvVarContainer con : m_envWidgets.values()) {
      con.loadEnvVarWidgets();
    }
    queueValidationHintsUpdate();
  }

  public void validate() throws ConfigurationException {
    validateExePath();
  }

  private void validateExePath() throws ConfigurationException {
    String bzrCmd = pathSelector.getText();
    if (!ShellCommandService.isValid(bzrCmd)) {
      pathSelector.getTextField().setForeground(Color.RED);
      throw new ConfigurationException(BzrVcsMessages.message("bzr4intellij.configuration.executable.error", bzrCmd));
    } else {
      pathSelector.getTextField().setForeground(Color.BLACK);
    }
  }

  private String getEnvValue(JCheckBox checkBox, JTextField textField) {
    if (checkBox.isSelected()) {
      return textField.getText();
    } else {
      return null;
    }
  }

  private void queueValidationHintsUpdate() {
    try {
      myQueue.queue(new Update("validation hints update") {
        public void run() {
          try {
            validateExePath();
          } catch (Throwable ignored) {
//            LOG.debug(ignored);
          }
        }
      });
    } catch (Throwable e) {
      LOG.debug(e);
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
    basePanel = new JPanel();
    basePanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
    basePanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("bzr4intellij.path_to_executable.text")));
    pathSelector = new TextFieldWithBrowseButton();
    pathSelector.setEnabled(true);
    panel1.add(pathSelector,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(100, -1), null,
            null, 0, false));
    pathDefaultButton = new JButton();
    this.$$$loadButtonText$$$(pathDefaultButton, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("configdialog.default_path_button.text"));
    panel1.add(pathDefaultButton,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1,
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages").getString("configpanel.exe_path.text"));
    panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    basePanel.add(spacer1,
        new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
    basePanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    panel2.setBorder(BorderFactory.createTitledBorder(
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("configpanel.miscellaneous_group.text")));
    m_trimAnnotationCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(m_trimAnnotationCheckBox,
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("configpanel.trim_annotation_author.text"));
    panel2.add(m_trimAnnotationCheckBox,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel2.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    m_modalErrorPopupEnabledCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(m_modalErrorPopupEnabledCheckBox,
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("configpanel.allow_modal_error_popup.text"));
    panel2.add(m_modalErrorPopupEnabledCheckBox,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
    basePanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    panel3.setBorder(BorderFactory.createTitledBorder(
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("configpanel.envvars_group.text")));
    m_envBzrEmailTextField = new JTextField();
    m_envBzrEmailTextField.setEnabled(true);
    m_envBzrEmailTextField.setText("");
    panel3.add(m_envBzrEmailTextField,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    final Spacer spacer3 = new Spacer();
    panel3.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    final Spacer spacer4 = new Spacer();
    panel3.add(spacer4, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    m_envBzrHomeTextField = new JTextField();
    m_envBzrHomeTextField.setEnabled(true);
    panel3.add(m_envBzrHomeTextField,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    m_envBzrEmailCheckBox = new JCheckBox();
    m_envBzrEmailCheckBox.setText("BZR_EMAIL");
    panel3.add(m_envBzrEmailCheckBox,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    m_envBzrHomeCheckBox = new JCheckBox();
    m_envBzrHomeCheckBox.setText("BZR_HOME");
    panel3.add(m_envBzrHomeCheckBox,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
    basePanel.add(panel4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    panel4.setBorder(BorderFactory.createTitledBorder(
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("configpanel.experimental_group.text")));
    m_granularExecLockingCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(m_granularExecLockingCheckBox,
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("configpanel.fine_grained_exec_locking.text"));
    panel4.add(m_granularExecLockingCheckBox,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer5 = new Spacer();
    panel4.add(spacer5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
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
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() {
    return basePanel;
  }

  private class EnvVarContainer {

    private String m_key;
    private JCheckBox m_checkBox;
    private JTextField m_textField;

    public EnvVarContainer(String key, JCheckBox checkBox, JTextField textField) {
      m_key = key;
      m_checkBox = checkBox;
      m_textField = textField;
      m_checkBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          updateEnvVarWidgets();
        }
      });
    }

    public boolean isModified() {
      return !(BzrCoreUtil.isEqual(getEnvValue(m_checkBox, m_textField), m_globalSettings.getBzrEnvVarSafe(m_key)));
    }

    public void updateEnvVarWidgets() {
      try {
        if (m_checkBox.isSelected()) {
          m_textField.setEditable(true);
          String val = m_globalSettings.getBzrEnvVar(m_key);
          m_textField.setText(val != null ? val : "");
        } else {
          m_textField.setEditable(false);
          String val = System.getenv(m_key);
          m_textField.setText(val != null ? val : "");
        }
      } catch (Throwable e) {
        LOG.error(e);
      }
    }

    public void loadEnvVarWidgets() {
      try {
        String val = m_globalSettings.getBzrEnvVar(m_key);
        if (val != null) {
          m_checkBox.setSelected(true);
          m_textField.setEditable(true);
          m_textField.setText(val);
        } else {
          m_checkBox.setSelected(false);
          m_textField.setEditable(false);
          val = System.getenv(m_key);
          m_textField.setText(val != null ? val : "");
        }
      } catch (Throwable e) {
        LOG.error(e);
      }
    }

    public void saveEnvVarWidgets() {
      m_globalSettings.setBzrEnvVarSafe(m_key, m_checkBox.isSelected() ? m_textField.getText() : null);
    }
  }
}
