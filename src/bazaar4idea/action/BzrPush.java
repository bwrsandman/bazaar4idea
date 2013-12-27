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

import bazaar4idea.BzrPlatformFacade;
import bazaar4idea.push.BzrPusher;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import bazaar4idea.command.BzrPushCommand;
import bazaar4idea.ui.BzrPushDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class BzrPush extends BzrRepositoryAction {

  @Override
  @NotNull
  protected String getActionName() {
    return "Push";
  }

  protected void perform(@NotNull final Project project,
                         @NotNull final List<VirtualFile> gitRoots,
                         @NotNull final VirtualFile defaultRoot,
                         final Set<VirtualFile> affectedRoots,
                         final List<VcsException> exceptions) throws VcsException {
    BzrPusher.showPushDialogAndPerformPush(project, ServiceManager.getService(project, BzrPlatformFacade.class));
  }

}
