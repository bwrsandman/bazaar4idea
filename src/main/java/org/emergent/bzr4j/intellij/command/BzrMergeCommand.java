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
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrAbstractResult;

import java.util.LinkedList;
import java.util.List;

public class BzrMergeCommand {

  private final Project project;
  private final VirtualFile repo;

  private String branch;
  private String revision;

  public BzrMergeCommand(Project project, VirtualFile repo) {
    this.project = project;
    this.repo = repo;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public BzrAbstractResult execute() {
    ShellCommandService commandService = ShellCommandService.getInstance(project);
    List<String> arguments = new LinkedList<String>();
    if (StringUtils.isNotBlank(revision)) {
      arguments.add("--rev");
      arguments.add(revision);
    } else if (StringUtils.isNotBlank(branch)) {
      arguments.add(branch);
    }
    return commandService.execute2(repo, "merge", arguments);
  }

}
