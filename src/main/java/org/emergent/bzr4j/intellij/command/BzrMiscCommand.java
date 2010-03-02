/*
 * Copyright (c) 2010 Patrick Woodworth
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

package org.emergent.bzr4j.intellij.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.BazaarRoot;
import org.emergent.bzr4j.core.cli.BzrAbstractResult;
import org.emergent.bzr4j.core.cli.BzrExecException;
import org.emergent.bzr4j.core.debug.DebugLogger;
import org.emergent.bzr4j.core.debug.DebugManager;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.BzrRootConverter;
import org.emergent.bzr4j.intellij.BzrUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Patrick Woodworth
 */
public class BzrMiscCommand {

  private static final DebugLogger LOG = DebugManager.getLogger(BzrMiscCommand.class);

  public static BzrRevisionNumber revno(Project project, @NotNull VirtualFile repo) {
    BazaarRoot bzrRoot = BzrRootConverter.INSTANCE.getBazaarRoot(project,repo);
    String revstr = bzrRoot == null ? null : bzrRoot.getRevno();
    if (revstr == null)
      return null;
    return BzrRevisionNumber.getLocalInstance(revstr);
  }

  public static boolean isIgnored(Project project, VirtualFile file) {
    ShellCommandService cmdSvc = ShellCommandService.getInstance(project);
    BzrIdeaExec handler = cmdSvc.createCommand(file, "is-ignored");
    if (handler == null)
      return false;

    handler.setExitValueValidationEnabled(false);
    handler.setStderrValidationEnabled(false);
    handler.addRelativePaths(file);
    BzrAbstractResult result = cmdSvc.execute(handler);
    if (result.getExitValue() == 1)
      return true;

    return false;
  }
}
