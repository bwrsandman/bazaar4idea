/*
 * Copyright (c) 2009 Emergent.org
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
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.BazaarException;
import org.emergent.bzr4j.core.IBazaarItemInfo;
import org.emergent.bzr4j.core.xmloutput.XmlOutputParser;
import bazaar4idea.BzrFile;
import bazaar4idea.data.BzrChange;
import bazaar4idea.data.BzrFileStatusEnum;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class BzrLsCommand extends BzrAbstractCommand {

  public BzrLsCommand(Project project) {
    super(project);
  }

  public List<BzrChange> execute(VirtualFile repo, VirtualFile target) {
    if (repo == null) {
      return Collections.emptyList();
    }

    List<BzrChange> changes = new ArrayList<BzrChange>();
    try {
      BzrIdeaExec handler = ShellCommandService.getInstance(project).createCommand(repo, "xmlls");
      handler.addArguments("--ignored");
      if (!repo.equals(target))
        handler.addRelativePaths(target);

      ShellCommandService service = ShellCommandService.getInstance(project);
      ShellCommandResult result = (ShellCommandResult)service.execute(handler);
      List<IBazaarItemInfo> infos = XmlOutputParser.parseXmlLs(result);
      for (IBazaarItemInfo info : infos) {
        File ioFile = new File(repo.getPath(), info.getPath());
        BzrFile change = new BzrFile(repo, ioFile);
        changes.add(new BzrChange(change, EnumSet.of(BzrFileStatusEnum.IGNORED)));
      }
    } catch (BazaarException e) {
      LOG.error(e);
    }
    return changes;
  }

}
