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
import com.intellij.util.Consumer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.command.BzrLogCommand;
import org.emergent.bzr4j.intellij.command.BzrMiscCommand;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class BzrHistoryProvider implements VcsHistoryProvider {

  @SuppressWarnings({ "UnusedDeclaration" })
  private static final Logger LOG = Logger.getInstance(BzrHistoryProvider.class.getName());

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

  public boolean isDateOmittable() {
    return false;
  }

  public String getHelpId() {
    return null;
  }

  public void reportAppendableHistory(
      FilePath filePath, final VcsAppendableHistorySessionPartner partner) throws VcsException {
    final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
    if (vcsRoot == null) {
      return;
    }

    BzrFile bzrFile = new BzrFile(vcsRoot, filePath);

    partner.reportCreatedEmptySession(createBzrHistorySession(Collections.<VcsFileRevision>emptyList(), vcsRoot));

    BzrLogCommand logCommand = new BzrLogCommand(project);
    logCommand.execute(bzrFile, new Consumer<VcsFileRevision>() {
      public void consume(VcsFileRevision revision) {
        partner.acceptRevision(revision);
      }
    });
  }

  public VcsHistorySession createSessionFor(FilePath filePath) throws VcsException {
    final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
    if (vcsRoot == null) {
      return null;
    }

    BzrFile bzrFile = new BzrFile(vcsRoot, filePath);
    BzrLogCommand logCommand = new BzrLogCommand(project);
    List<VcsFileRevision> revisions = logCommand.execute(bzrFile);
    return createBzrHistorySession(revisions, vcsRoot);
  }

  private VcsAbstractHistorySession createBzrHistorySession(
      final List<VcsFileRevision> revisions, final VirtualFile vcsRoot) {
    return new VcsAbstractHistorySession(revisions) {
      @Override
      protected VcsRevisionNumber calcCurrentRevisionNumber() {
        return BzrMiscCommand.revno(project,vcsRoot);
      }

      public HistoryAsTreeProvider getHistoryAsTreeProvider() {
        return null;
      }

      public VcsHistorySession copy() {
        return this;
      }
    };
  }

  public boolean supportsHistoryForDirectories() {
    return true;
  }
}
