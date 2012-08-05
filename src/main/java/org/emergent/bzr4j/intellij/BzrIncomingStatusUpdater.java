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
package org.emergent.bzr4j.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import org.emergent.bzr4j.intellij.command.BzrIncomingCommand;
import org.emergent.bzr4j.intellij.ui.BzrChangesetStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

class BzrIncomingStatusUpdater implements BzrUpdater {

  private static final int LIMIT = 100;

  private final BzrChangesetStatus status;
  private final BzrProjectSettings projectSettings;

  public BzrIncomingStatusUpdater(BzrChangesetStatus status, BzrProjectSettings projectSettings) {
    this.status = status;
    this.projectSettings = projectSettings;
  }

  public void update(final Project project) {
    if (!projectSettings.isCheckIncoming()) {
      return;
    }
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        new Task.Backgroundable(project, "Checking Incoming Changesets", true) {
          public void run(@NotNull ProgressIndicator indicator) {
            BzrIncomingCommand command = new BzrIncomingCommand(project);
            VcsRoot[] roots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
            List<BzrFileRevision> changesets = new LinkedList<BzrFileRevision>();
            for (VcsRoot root : roots) {
              @SuppressWarnings("ConstantConditions")
              BzrFile hgFile = new BzrFile(root.getPath(), new File("."));
              changesets.addAll(command.execute(hgFile, LIMIT - changesets.size()));
            }
            status.setChanges(changesets.size(), new IncomingChangesetFormatter(changesets));
          }
        }.queue();
      }
    });
  }

  private final class IncomingChangesetFormatter implements BzrChangesetStatus.ChangesetWriter {

    private final StringBuilder builder = new StringBuilder();

    private IncomingChangesetFormatter(List<BzrFileRevision> changesets) {
      builder.append("<html>");
      builder.append("<b>Incoming changesets</b>:<br>");
      for (BzrFileRevision changeset : changesets) {
        builder
            .append(changeset.getRevisionNumber().asString()).append(" ")
            .append(changeset.getCommitMessage()).append(" ")
            .append("(").append(changeset.getAuthor()).append(")<br>");
      }
      builder.append("</html>");
    }

    public String asString() {
      return builder.toString();
    }
  }

}
