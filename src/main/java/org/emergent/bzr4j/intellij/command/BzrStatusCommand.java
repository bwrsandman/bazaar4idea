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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.BazaarException;
import org.emergent.bzr4j.core.IBazaarStatus;
import org.emergent.bzr4j.core.xmloutput.XmlOutputParser;
import org.emergent.bzr4j.core.xmloutput.XmlStatusResult;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.data.BzrChange;
import org.emergent.bzr4j.intellij.data.BzrFileStatusEnum;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class BzrStatusCommand extends BzrAbstractCommand {

  private boolean includeUnknown = true;
  private boolean includeIgnored = true;

  public BzrStatusCommand(Project project) {
    super(project);
  }

  public void setIncludeUnknown(boolean includeUnknown) {
    this.includeUnknown = includeUnknown;
  }

  public void setIncludeIgnored(boolean includeIgnored) {
    this.includeIgnored = includeIgnored;
  }

  public Set<BzrChange> execute(VirtualFile repo) {
    return execute(repo,repo);
  }

  public Set<BzrChange> execute(VirtualFile repo, VirtualFile target) {
    if (repo == null) {
      return Collections.emptySet();
    }

    BzrIntellijHandler handler = ShellCommandService.getInstance(project).createCommand(repo, "xmlstatus");
    if (!repo.equals(target))
      handler.addRelativePaths(target);
    ShellCommandService service = ShellCommandService.getInstance(project);
    ShellCommandResult result = service.execute(handler);
    Set<BzrChange> changes = new HashSet<BzrChange>();
    try {
      XmlStatusResult parser = XmlOutputParser.parseXmlStatus(result);
      Set<IBazaarStatus> statii = parser.getStatusSet();
      for (IBazaarStatus bzrStatus : statii) {
        EnumSet<BzrFileStatusEnum> statusSet = EnumSet.noneOf(BzrFileStatusEnum.class);
        String shortStatus = bzrStatus.getShortStatus();
        for (char c : shortStatus.toCharArray()) {
          BzrFileStatusEnum status = BzrFileStatusEnum.valueOf(c);
          if (status == null)
            continue;
          statusSet.add(status);
        }
        File statusBranchRoot = bzrStatus.getBranchRoot();
        String newPath = bzrStatus.getPath();
        LOG.debug(String.format("newpath: \"%s\"", newPath));
        File ioFile = new File(statusBranchRoot, newPath);
        BzrChange change = new BzrChange(new BzrFile(repo, ioFile), statusSet);
        String oldPath = bzrStatus.getPreviousPath();
        if (!StringUtil.isEmpty(oldPath)) {
          LOG.debug(String.format("oldpath: \"%s\"", oldPath));
          change.setBeforeFile(new BzrFile(repo, new File(statusBranchRoot, oldPath)));
        }
        changes.add(change);
      }
    } catch (BazaarException e) {
      LOG.error(e);
      return Collections.emptySet();
    }
    return changes;
  }
}
