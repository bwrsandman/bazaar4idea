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
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class BzrTagCreateCommand {

  private final Project project;
  private final VirtualFile repo;
  private final String tagName;

  public BzrTagCreateCommand(Project project, @NotNull VirtualFile repo, String tagName) {
    this.project = project;
    this.repo = repo;
    this.tagName = tagName;
  }

  public BzrStandardResult execute() throws BzrCommandException {
    if (StringUtils.isBlank(tagName)) {
      throw new BzrCommandException("tag name is empty");
    }
    return ShellCommandService.getInstance(project).execute2(repo, "tag", Arrays.asList(tagName));
  }

}
