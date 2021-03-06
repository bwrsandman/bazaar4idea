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
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.core.BazaarException;
import org.emergent.bzr4j.core.IBazaarInfo;
import org.emergent.bzr4j.core.xmloutput.XmlOutputParser;

import java.util.LinkedList;
import java.util.List;

public class BzrInfoCommand extends BzrAbstractCommand {

  public BzrInfoCommand(Project project) {
    super(project);
  }

  public final IBazaarInfo execute(VirtualFile file) {
    VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
    if (vcsRoot == null) {
      return null;
    }

    ShellCommandService commandService = ShellCommandService.getInstance(project);

    List<String> arguments = new LinkedList<String>();
    arguments.add("-v");
//        arguments.add(bzrFile.getRelativePath());
    ShellCommandResult result = (ShellCommandResult)commandService.execute(vcsRoot, "xmlinfo", arguments);
    IBazaarInfo protorevs = null;
    try {
      protorevs = XmlOutputParser.parseXmlInfo(result);
    } catch (BazaarException e) {
      e.printStackTrace();
    }
    return protorevs;
  }

}
