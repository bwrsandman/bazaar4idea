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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import bazaar4idea.BzrRevisionNumber;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BzrHeadsCommand {

  private static final Logger LOG = Logger.getInstance(BzrHeadsCommand.class.getName());

  private final Project project;
  private final VirtualFile repo;

  public BzrHeadsCommand(Project project, @NotNull VirtualFile repo) {
    this.project = project;
    this.repo = repo;
  }

  public List<BzrRevisionNumber> execute(String branch) {
    ShellCommandService command = ShellCommandService.getInstance(project);

    BzrStandardResult result = command.execute2(repo, "heads",
        Arrays.asList("--template", "{rev}|{node|short}\\n", branch));

    List<BzrRevisionNumber> heads = new ArrayList<BzrRevisionNumber>();
    for (String line : result.getStdOutAsLines()) {
      try {
        String[] parts = StringUtils.split(line, '|');
        heads.add(BzrRevisionNumber.getInstance(parts[0], parts[1]));
      } catch (NumberFormatException e) {
        LOG.warn("Unexpected head line '" + line + "'");
      }
    }
    return heads;
  }

}
