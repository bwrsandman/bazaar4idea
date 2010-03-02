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
package org.emergent.bzr4j.intellij.provider;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.HistoryAsTreeProvider;
import com.intellij.openapi.vcs.history.VcsAbstractHistorySession;
import com.intellij.openapi.vcs.history.VcsAppendableHistorySessionPartner;
import com.intellij.openapi.vcs.history.VcsDependentHistoryComponents;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.command.BzrLogCommand;
import org.emergent.bzr4j.intellij.command.BzrMiscCommand;

import javax.swing.*;
import java.util.List;

public class BzrHistoryProvider implements VcsHistoryProvider {

  private static final Logger LOG = Logger.getInstance(BzrHistoryProvider.class.getName());

  private static final int DEFAULT_LIMIT = 500;

  private final Project project;

  public BzrHistoryProvider(Project project) {
    this.project = project;
  }

  public VcsDependentHistoryComponents getUICustomization(VcsHistorySession session,
      JComponent forShortcutRegistration) {
    return VcsDependentHistoryComponents.createOnlyColumns(new ColumnInfo[0]);
  }

  public AnAction[] getAdditionalActions(Runnable runnable) {
    return new AnAction[0];
  }

  public void reportAppendableHistory(
      FilePath filePath, VcsAppendableHistorySessionPartner partner) throws VcsException {
    // todo make this lazily read the revisions from bzr's output
    final VcsAbstractHistorySession session = createSessionFor(filePath);
    partner.reportCreatedEmptySession(session);
  }

  public boolean isDateOmittable() {
    return false;
  }

  public String getHelpId() {
    return null;
  }

  public VcsAbstractHistorySession createSessionFor(FilePath filePath) throws VcsException {
    final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
    if (vcsRoot == null) {
      return null;
    }
    BzrFile hgFile = new BzrFile(vcsRoot, filePath);
    BzrLogCommand logCommand = new BzrLogCommand(project);
    List<VcsFileRevision> result = logCommand.execute(hgFile, DEFAULT_LIMIT);
    return new VcsAbstractHistorySession(result) {
      @Override
      protected VcsRevisionNumber calcCurrentRevisionNumber() {
        return BzrMiscCommand.revno(project,vcsRoot);
      }

      public HistoryAsTreeProvider getHistoryAsTreeProvider() {
        return null;
      }
    };
  }

  public boolean supportsHistoryForDirectories() {
    return true;
  }
}
