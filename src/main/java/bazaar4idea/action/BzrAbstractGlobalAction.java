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
package bazaar4idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsImplUtil;
import bazaar4idea.BzrVcs;
import bazaar4idea.command.BzrCommandException;
import bazaar4idea.util.BzrDebug;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

abstract class BzrAbstractGlobalAction extends AnAction {

  protected abstract BzrGlobalCommandBuilder getBzrGlobalCommandBuilder(Project project);

  protected boolean isExperimental() {
    return false;
  }

  public void actionPerformed(AnActionEvent event) {
    Project project = event.getData(DataKeys.PROJECT);
    if (project == null) {
      return;
    }

    BzrGlobalCommand command = getBzrGlobalCommandBuilder(project).build(findRepos(project));
    if (command == null) {
      return;
    }
    try {
      command.execute();
    } catch (BzrCommandException e) {
      VcsImplUtil.showErrorMessage(project, e.getMessage(), "Error");
    }
    VcsDirtyScopeManager vcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
    vcsDirtyScopeManager.dirDirtyRecursively(command.getRepo());
    command.getRepo().refresh(true, true);
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

    if (vcs == null || !vcs.isStarted()) {
      presentation.setEnabled(false);
    }
  }

  private List<VirtualFile> findRepos(Project project) {
    List<VirtualFile> repos = new LinkedList<VirtualFile>();
    VcsRoot[] roots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
    for (VcsRoot root : roots) {
      //noinspection ConstantConditions
      if (BzrVcs.VCS_NAME.equals(root.getVcs().getName())) {
        repos.add(root.getPath());
      }
    }
    return repos;
  }

  protected interface BzrGlobalCommand {

    VirtualFile getRepo();

    void execute() throws BzrCommandException;
  }

  protected interface BzrGlobalCommandBuilder {

    @Nullable
    BzrGlobalCommand build(Collection<VirtualFile> repos);
  }

}
