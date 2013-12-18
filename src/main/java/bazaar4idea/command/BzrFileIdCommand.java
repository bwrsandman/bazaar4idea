/*
 * Copyright (c) 2009 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package bazaar4idea.command;

import com.intellij.openapi.project.Project;
import org.emergent.bzr4j.core.cli.BzrAbstractResult;
import bazaar4idea.BzrFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author Patrick Woodworth
 */
public class BzrFileIdCommand {

  private final Project project;

  public BzrFileIdCommand(Project project) {
    this.project = project;
  }

  public boolean isVersioned(@NotNull BzrFile hgFile) {
    BzrIdeaExec shellCmd = ShellCommandService.getInstance(project).createCommand(hgFile.getRepo(), "file-id");
    shellCmd.addArguments(Arrays.asList(hgFile.getRelativePath()));
    shellCmd.setExitValueValidationEnabled(false);
    shellCmd.setStderrValidationEnabled(false);
    ShellCommandService commandService = ShellCommandService.getInstance(project);
    BzrAbstractResult result = commandService.execute(shellCmd);
    if (result.getExitValue() != 0)
      return false;
//    if (result.getErrorLines().size() > 0)
//      return false;
    return true;
  }
}
