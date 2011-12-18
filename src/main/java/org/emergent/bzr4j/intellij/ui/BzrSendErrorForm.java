package org.emergent.bzr4j.intellij.ui;

import com.intellij.CommonBundle;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TextComponentUndoProvider;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.emergent.bzr4j.intellij.util.BzrErrorReportConfigurable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class BzrSendErrorForm extends DialogWrapper {

  private static final String ERROR_REPORT = BzrVcsMessages.message("error.report.title");

  private static final Pattern EMAIL_VALIDATION_PATTERN = Pattern.compile(".+@.+\\.[a-z]+");

  private JTextField myItnLoginTextField;
  private JPasswordField myItnPasswordTextField;
  private JCheckBox myRememberITNPasswordCheckBox;

  public void storeInfo() {
    BzrErrorReportConfigurable reportConf = BzrErrorReportConfigurable.getInstance();
    reportConf.EMAIL_ADDRESS = myItnEmailTextField.getText();
    reportConf.SMTP_SERVER = myItnServerTextField.getText();
    reportConf.AUTH_USERNAME = myItnLoginTextField.getText();
    reportConf.setPlainItnPassword(new String(myItnPasswordTextField.getPassword()));
    reportConf.SAVE_PASSWORD = myRememberITNPasswordCheckBox.isSelected();
  }

  public void loadInfo() {
    BzrErrorReportConfigurable reportConf = BzrErrorReportConfigurable.getInstance();
    myItnEmailTextField.setText(reportConf.EMAIL_ADDRESS);
    myItnServerTextField.setText(reportConf.SMTP_SERVER);
    myItnLoginTextField.setText(reportConf.AUTH_USERNAME);
    myItnPasswordTextField.setText(reportConf.getPlainItnPassword());
    myRememberITNPasswordCheckBox.setSelected(reportConf.SAVE_PASSWORD);
    validateSendable();
  }

  public BzrSendErrorForm() throws HeadlessException {
    super(false);
    init();
  }

  protected JPanel myMainPanel;
  protected JTextArea myErrorDescriptionTextArea;
  private JTextField myItnServerTextField;
  private JTextField myItnEmailTextField;
  private Action mySendAction;
  private Action myCancelAction;

  private boolean myShouldSend = false;

  private void validateSendable() {
    if (EMAIL_VALIDATION_PATTERN.matcher(myItnEmailTextField.getText()).find()) {
      mySendAction.setEnabled(true);
    } else {
      mySendAction.setEnabled(false);
    }
  }

  public boolean isShouldSend() {
    return myShouldSend;
  }

  protected String getDimensionServiceKey() {
//    return "#com.intellij.diagnostic.AbstractSendErrorDialog";
    return "#" + BzrSendErrorForm.class.getName();
  }

  protected void init() {
    setTitle(ERROR_REPORT);
    getContentPane().add(myMainPanel);
    mySendAction = new AbstractAction(BzrVcsMessages.message("diagnostic.error.report.send")) {
      public void actionPerformed(ActionEvent e) {
        myShouldSend = true;
        storeInfo();
        Disposer.dispose(myDisposable);
      }
    };
    mySendAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
    mySendAction.putValue(DialogWrapper.DEFAULT_ACTION, Boolean.TRUE.toString());
    mySendAction.setEnabled(false);
    myCancelAction = new AbstractAction(CommonBundle.getCancelButtonText()) {
      public void actionPerformed(ActionEvent e) {
        myShouldSend = false;
        Disposer.dispose(myDisposable);
      }
    };
    myCancelAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));

    DocumentAdapter sendValidationDocListener = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        validateSendable();
      }
    };

    myItnEmailTextField.getDocument().addDocumentListener(sendValidationDocListener);
    myItnServerTextField.getDocument().addDocumentListener(sendValidationDocListener);

    loadInfo();

    new TextComponentUndoProvider(myErrorDescriptionTextArea);
    super.init();
  }

  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  protected Action[] createActions() {
    return new Action[] { mySendAction, myCancelAction };
  }

  public String getErrorDescription() {
    return myErrorDescriptionTextArea.getText();
  }

  public void setErrorDescription(String description) {
    myErrorDescriptionTextArea.setText(description);
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
    myMainPanel = new JPanel();
    myMainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("diagnostic.error.report.prompt"));
    myMainPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JScrollPane scrollPane1 = new JScrollPane();
    myMainPanel.add(scrollPane1,
        new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    myErrorDescriptionTextArea = new JTextArea();
    myErrorDescriptionTextArea.setColumns(40);
    myErrorDescriptionTextArea.setLineWrap(true);
    myErrorDescriptionTextArea.setRows(5);
    myErrorDescriptionTextArea.setTabSize(4);
    myErrorDescriptionTextArea.setText("");
    myErrorDescriptionTextArea.setWrapStyleWord(true);
    scrollPane1.setViewportView(myErrorDescriptionTextArea);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(2, 1, new Insets(2, 4, 0, 4), -1, -1));
    myMainPanel.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("diagnostic.error.report.login.group")));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
    panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("diagnostic.error.report.login.password"));
    panel2.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("diagnostic.error.report.login.name"));
    panel2.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myItnLoginTextField = new JTextField();
    panel2.add(myItnLoginTextField,
        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    myItnPasswordTextField = new JPasswordField();
    panel2.add(myItnPasswordTextField,
        new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    final JLabel label4 = new JLabel();
    this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("diagnostic.error.report.smtp.server"));
    panel2.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label5 = new JLabel();
    this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("diagnostic.error.report.email"));
    panel2.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myItnServerTextField = new JTextField();
    myItnServerTextField.setText("");
    panel2.add(myItnServerTextField,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    myItnEmailTextField = new JTextField();
    panel2.add(myItnEmailTextField,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    myRememberITNPasswordCheckBox = new JCheckBox();
    this.$$$loadButtonText$$$(myRememberITNPasswordCheckBox,
        ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
            .getString("diagnostic.error.report.login.remember.password.checkbox"));
    panel1.add(myRememberITNPasswordCheckBox,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label6 = new JLabel();
    this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("org/emergent/bzr4j/intellij/BzrVcsMessages")
        .getString("diagnostic.error.report.description"));
    myMainPanel.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
    return myMainPanel;
  }
}
