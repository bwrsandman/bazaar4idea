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
package org.emergent.bzr4j.intellij.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.emergent.bzr4j.intellij.command.BzrCommandException;
import org.emergent.bzr4j.intellij.command.BzrTagCreateCommand;
import org.emergent.bzr4j.intellij.ui.BzrTagDialog;

import java.util.Collection;

public class BzrCreateTagAction extends BzrAbstractGlobalAction {

  @Override
  protected boolean isExperimental() {
    return true;
  }

  protected BzrGlobalCommandBuilder getBzrGlobalCommandBuilder(final Project project) {
    return new BzrGlobalCommandBuilder() {
      public BzrGlobalCommand build(Collection<VirtualFile> repos) {
        BzrTagDialog dialog = new BzrTagDialog(project);
        dialog.setRoots(repos);
        dialog.show();
        if (dialog.isOK()) {
          return buildCommand(dialog, project);
        }
        return null;
      }
    };
  }

  private BzrGlobalCommand buildCommand(final BzrTagDialog dialog, final Project project) {
    return new BzrGlobalCommand() {
      public VirtualFile getRepo() {
        return dialog.getRepository();
      }

      public void execute() throws BzrCommandException {
        BzrStandardResult result =
            new BzrTagCreateCommand(project, dialog.getRepository(), dialog.getTagName()).execute();

        new BzrCommandResultNotifier(project).process(result);
      }
    };
  }

}
