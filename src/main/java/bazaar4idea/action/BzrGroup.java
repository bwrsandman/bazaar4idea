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

package bazaar4idea.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.actions.StandardVcsGroup;
import bazaar4idea.util.BzrDebug;
import bazaar4idea.BzrVcs;

public class BzrGroup extends StandardVcsGroup {

  public AbstractVcs getVcs(Project project) {
    return ProjectLevelVcsManager.getInstance(project).findVcsByName(BzrVcs.VCS_NAME);
  }

  @Override
  public String getVcsName(final Project project) {
    return BzrVcs.VCS_NAME;
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);

    Presentation presentation = e.getPresentation();

    if (!BzrDebug.EXPERIMENTAL_ENABLED && isExperimental()) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }

    final DataContext dataContext = e.getDataContext();

    Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    BzrVcs vcs = (BzrVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(BzrVcs.VCS_NAME);

    if (!vcs.isStarted()) {
      presentation.setEnabled(false);
    }
  }

  protected boolean isExperimental() {
    return false;
  }
}
