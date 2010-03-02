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
package org.emergent.bzr4j.intellij.provider.commit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.util.ui.EmptyIcon;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class BzrCommitExecutor implements CommitExecutor {

  private final Project project;

  public BzrCommitExecutor(Project project) {
    this.project = project;
  }

  @NotNull
  public Icon getActionIcon() {
    return new EmptyIcon(1);
  }

  @Nls
  public String getActionText() {
    Object[] params = new Object[] { };
    return BzrVcsMessages.message("bzr4intellij.commit.repository.title", params);
  }

  @Nls
  public String getActionDescription() {
    Object[] params = new Object[] { };
    return BzrVcsMessages.message("bzr4intellij.commit.repository.body", params);
  }

  @NotNull
  public CommitSession createCommitSession() {
    return new BzrCommitSession(project);
  }

}
