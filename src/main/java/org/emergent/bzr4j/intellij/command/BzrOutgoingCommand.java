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
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.emergent.bzr4j.intellij.BzrFile;

import java.util.Arrays;

public class BzrOutgoingCommand extends BzrRevisionsCommand {

  public BzrOutgoingCommand(Project project) {
    super(project);
  }

  protected BzrStandardResult execute(ShellCommandService service, VirtualFile repo,
      String template, int limit, BzrFile hgFile) {
    return service.execute2(repo, "outgoing",
        Arrays.asList("--newest-first", "--template", template, "--limit", String.valueOf(limit)));
  }

}
