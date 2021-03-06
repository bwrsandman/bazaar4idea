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

import com.intellij.openapi.util.IconLoader;
import org.apache.commons.lang.StringUtils;
import bazaar4idea.i18n.BzrBundle;

import javax.swing.*;

public class BzrCurrentBranchStatus extends JLabel {

  private static final Icon BAZAAR_ICON = IconLoader.getIcon("/org/emergent/bzr4j/intellij/bzr.png");

  public BzrCurrentBranchStatus() {
    super(BAZAAR_ICON, SwingConstants.TRAILING);
    setVisible(false);
  }

  public void setCurrentBranch(String branch) {
    Object[] params1 = new Object[] { branch };
    String statusText = StringUtils.isNotBlank(branch)
        ? BzrBundle.message("bzr4intellij.status.currentBranch.text", params1) : "";

    Object[] params = new Object[] { };
    String toolTipText = StringUtils.isNotBlank(branch)
        ? BzrBundle.message("bzr4intellij.status.currentBranch.description", params) : "";

    setVisible(StringUtils.isNotBlank(branch));
    setText(statusText);
    setToolTipText(toolTipText);
  }

}
