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
package org.emergent.bzr4j.intellij.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.emergent.bzr4j.intellij.BzrVcs;
import org.emergent.bzr4j.intellij.data.BzrUrl;
import org.emergent.bzr4j.intellij.ui.BzrUsernamePasswordDialog;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public class BzrPullCommand {

  private final Project project;
  private final VirtualFile repo;

  private String source;
  private String revision;
  private boolean update = true;
  private boolean rebase = !update;

  public BzrPullCommand(Project project, @NotNull VirtualFile repo) {
    this.project = project;
    this.repo = repo;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public void setUpdate(boolean update) {
    this.update = update;
  }

  public void setRebase(boolean rebase) {
    this.rebase = rebase;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public BzrStandardResult execute() {
    List<String> arguments = new LinkedList<String>();
    if (update) {
      arguments.add("--update");
    } else if (rebase) {
      arguments.add("--rebase");
    }

    if (StringUtils.isNotBlank(revision)) {
      arguments.add("--rev");
      arguments.add(revision);
    }

    arguments.add(source);

    BzrStandardResult result = ShellCommandService.getInstance(project).execute2(repo, "pull", arguments);
    if (BzrErrorUtil.isAbort(result) && BzrErrorUtil.isAuthorizationRequiredAbort(result)) {
      try {
        BzrUrl hgUrl = new BzrUrl(source);
        if (hgUrl.supportsAuthentication()) {
          BzrUsernamePasswordDialog dialog = new BzrUsernamePasswordDialog(project);
          dialog.setUsername(hgUrl.getUsername());
          dialog.show();
          if (dialog.isOK()) {
            hgUrl.setUsername(dialog.getUsername());
            hgUrl.setPassword(String.valueOf(dialog.getPassword()));
            arguments.set(arguments.size() - 1, hgUrl.asString());
            result = ShellCommandService.getInstance(project).execute2(repo, "pull", arguments);
          }
        }
      } catch (URISyntaxException e) {
        VcsUtil.showErrorMessage(project, "Invalid source: " + source, "Error");
      }
    }

    project.getMessageBus().syncPublisher(BzrVcs.INCOMING_TOPIC).update(project);

    return result;
  }

}
