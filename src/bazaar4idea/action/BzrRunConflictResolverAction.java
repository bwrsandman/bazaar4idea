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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import bazaar4idea.BzrRevisionNumber;
import bazaar4idea.command.BzrHeadsCommand;
import bazaar4idea.command.BzrTagBranchCommand;
import bazaar4idea.provider.update.BzrConflictResolver;
import bazaar4idea.ui.BzrRunConflictResolverDialog;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BzrRunConflictResolverAction extends BzrAbstractGlobalAction {

  @Override
  protected boolean isExperimental() {
    return true;
  }

  protected BzrGlobalCommandBuilder getBzrGlobalCommandBuilder(final Project project) {
    return new BzrGlobalCommandBuilder() {
      public BzrGlobalCommand build(Collection<VirtualFile> repos) {
        BzrRunConflictResolverDialog dialog = new BzrRunConflictResolverDialog(project);
        dialog.setRoots(repos);
        dialog.show();

        if (dialog.isOK()) {
          return buildCommand(dialog, project);
        }
        return null;
      }
    };
  }

  private BzrGlobalCommand buildCommand(BzrRunConflictResolverDialog dialog, final Project project) {
    final VirtualFile repository = dialog.getRepository();
    return new BzrGlobalCommand() {
      public VirtualFile getRepo() {
        return repository;
      }

      public void execute() {
        String currentBranch = new BzrTagBranchCommand(project, repository).getCurrentBranch();
        if (currentBranch == null) {
          return;
        }
        List<BzrRevisionNumber> heads =
            new BzrHeadsCommand(project, repository).execute(currentBranch);

        new BzrConflictResolver(project, Collections.max(heads), Collections.min(heads))
            .resolve(repository);
      }
    };
  }

}
