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
package bazaar4idea;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import bazaar4idea.ui.BzrConfigurationProjectPanel;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

public class BzrProjectConfigurable implements Configurable {

  private final Project m_project;

  private BzrConfigurationProjectPanel m_bzrConfigurationProjectPanel;

  public BzrProjectConfigurable(Project project) {
    m_project = project;
  }

  @Nls
  public String getDisplayName() {
    return BzrVcsMessages.message("bzr4intellij.bazaar");
  }

  public Icon getIcon() {
    return BzrVcs.BAZAAR_ICON;
  }

  public String getHelpTopic() {
    // This is supposed to be nullable, but there's a bug in VcsManagerConfigurable which requires this be not null
    return "project.propVCSSupport.VCSs.Bazaar";
  }

  public JComponent createComponent() {
    if (m_bzrConfigurationProjectPanel == null) {
      BzrProjectSettings projectSettings = BzrProjectSettings.getInstance(m_project);
      m_bzrConfigurationProjectPanel = new BzrConfigurationProjectPanel(projectSettings);
    }
    return m_bzrConfigurationProjectPanel.getPanel();
  }

  public boolean isModified() {
    return m_bzrConfigurationProjectPanel.isModified();
  }

  public void apply() throws ConfigurationException {
    m_bzrConfigurationProjectPanel.saveSettings();
    BzrVcs.getInstance(m_project).getMyRootTracker().directoryMappingChanged();
  }

  public void reset() {
    m_bzrConfigurationProjectPanel.loadSettings();
  }

  public void disposeUIResources() {
    m_bzrConfigurationProjectPanel = null;
  }

}
