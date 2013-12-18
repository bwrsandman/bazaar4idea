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
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.BazaarRoot;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import bazaar4idea.BzrFile;
import bazaar4idea.BzrRevisionNumber;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class BzrCatCommand extends BzrAbstractCommand {

  public BzrCatCommand(Project project) {
    super(project);
  }

  public String execute(BzrFile hgFile, BzrRevisionNumber vcsRevisionNumber, Charset charset) {
    List<String> arguments = new LinkedList<String>();
    if (vcsRevisionNumber != null && StringUtils.isNotBlank(vcsRevisionNumber.asString())) {
      arguments.add("-r");
      arguments.add(vcsRevisionNumber.asString());
    }
    arguments.add(hgFile.getRelativePath());

    ShellCommandService service = ShellCommandService.getInstance(project);
    BzrStandardResult result = service.execute(hgFile.getRepo(), new BzrStandardResult(charset), "cat", arguments);
    return result.getStdOutAsString();
  }

  public String execute(File vcsRootFile, File path, BzrRevisionNumber vcsRevisionNumber, Charset charset) {
    BzrIdeaExec handler = new BzrIdeaExec(BazaarRoot.findBranchLocation(vcsRootFile), "cat");
    if (vcsRevisionNumber != null && StringUtils.isNotBlank(vcsRevisionNumber.asString())) {
      handler.addArguments("-r",vcsRevisionNumber.asString());
    }
    handler.addArguments(path.getPath());
    ShellCommandService service = ShellCommandService.getInstance(project);
    BzrStandardResult result = service.execute(handler, new BzrStandardResult(charset));
    return result.getStdOutAsString();
  }

  public String execute(File file, BzrRevisionNumber vcsRevisionNumber, Charset charset) {
    BzrIdeaExec handler = new BzrIdeaExec(BazaarRoot.findBranchLocation(file), "cat");
    if (vcsRevisionNumber != null && StringUtils.isNotBlank(vcsRevisionNumber.asString())) {
      handler.addArguments("-r",vcsRevisionNumber.asString());
    }
    handler.addRelativePaths(file);
    ShellCommandService service = ShellCommandService.getInstance(project);
    BzrStandardResult result = service.execute(handler, new BzrStandardResult(charset));
    return result.getStdOutAsString();
  }

  public String execute(File vcsRoot, String relpath, BzrRevisionNumber vcsRevisionNumber, Charset charset) {
    BzrIdeaExec handler = new BzrIdeaExec(BazaarRoot.findBranchLocation(vcsRoot), "cat");
    if (vcsRevisionNumber != null && StringUtils.isNotBlank(vcsRevisionNumber.asString())) {
      handler.addArguments("-r",vcsRevisionNumber.asString());
    }
    handler.addArguments(relpath);
    ShellCommandService service = ShellCommandService.getInstance(project);
    BzrStandardResult result = service.execute(handler, new BzrStandardResult(charset));
    return result.getStdOutAsString();
  }
}
