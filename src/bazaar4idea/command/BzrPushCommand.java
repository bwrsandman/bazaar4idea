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
package bazaar4idea.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsImplUtil;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import bazaar4idea.BzrVcs;
import bazaar4idea.data.BzrUrl;
import bazaar4idea.ui.BzrUsernamePasswordDialog;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public class BzrPushCommand {

  private final Project project;
  private final VirtualFile repo;
  private final String destination;

  private String revision;

  public BzrPushCommand(Project project, @NotNull VirtualFile repo, String destination) {
    this.project = project;
    this.repo = repo;
    this.destination = destination;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public BzrStandardResult execute() {
    List<String> arguments = new LinkedList<String>();
    if (StringUtils.isNotBlank(revision)) {
      arguments.add("-r");
      arguments.add(revision);
    }
    arguments.add(destination);
    BzrStandardResult result = ShellCommandService.getInstance(project).execute2(repo, "push", arguments);
    if (BzrErrorUtil.isAbort(result) && BzrErrorUtil.isAuthorizationRequiredAbort(result)) {
      try {
        BzrUrl hgUrl = new BzrUrl(destination);
        if (hgUrl.supportsAuthentication()) {
          BzrUsernamePasswordDialog dialog = new BzrUsernamePasswordDialog(project);
          dialog.setUsername(hgUrl.getUsername());
          dialog.show();
          if (dialog.isOK()) {
            hgUrl.setUsername(dialog.getUsername());
            hgUrl.setPassword(String.valueOf(dialog.getPassword()));
            arguments.set(arguments.size() - 1, hgUrl.asString());
            result = ShellCommandService.getInstance(project).execute2(repo, "push", arguments);
          }
        }
      } catch (URISyntaxException e) {
        VcsImplUtil.showErrorMessage(project, "Invalid destination: " + destination, "Error");
      }
    }

    project.getMessageBus().syncPublisher(BzrVcs.OUTGOING_TOPIC).update(project);

    return result;
  }
}
