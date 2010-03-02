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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.intellij.BzrContentRevision;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.command.BzrMiscCommand;

public class BzrDiffProvider implements DiffProvider {

  private static final Logger LOG = Logger.getInstance(BzrDiffProvider.class.getName());

  private final Project project;

  public BzrDiffProvider(Project project) {
    this.project = project;
  }

  public VcsRevisionNumber getCurrentRevision(VirtualFile file) {
    VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
    if (vcsRoot == null) {
      return null;
    }

    BzrRevisionNumber currentRevision = BzrMiscCommand.revno(project,vcsRoot);
    if (currentRevision == null) {
      return null;
    }

//        if (currentRevision.isWorkingVersion()) {
//            return command.parent(vcsRoot);
//        }
    return currentRevision;
  }

  public ItemLatestState getLastRevision(VirtualFile file) {
    VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
    if (vcsRoot == null) {
      return null;
    }
    BzrRevisionNumber revision = (BzrRevisionNumber)getCurrentRevision(file);
    if (revision == null) {
      return null;
    }
    return new ItemLatestState(revision, file.exists(), true);
  }

  public ItemLatestState getLastRevision(FilePath filePath) {
    VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
    if (vcsRoot == null) {
      return null;
    }

    BzrRevisionNumber currentRevision = BzrMiscCommand.revno(project,vcsRoot);
    if (currentRevision == null) {
      return null;
    }

    boolean fileExists = filePath.getIOFile().exists();
//        if (currentRevision.isWorkingVersion()) {
//            return new ItemLatestState(command.parent(vcsRoot), fileExists, true);
//        }

    return new ItemLatestState(currentRevision, fileExists, true);
  }

  public VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot) {
    return BzrMiscCommand.revno(project,vcsRoot);
  }

  public ContentRevision createFileContent(VcsRevisionNumber revisionNumber, VirtualFile file) {
    if (file == null) {
      return null;
    }

    VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
    if (vcsRoot == null) {
      return null;
    }

    BzrRevisionNumber hgRevisionNumber = (BzrRevisionNumber)revisionNumber;
//        if (hgRevisionNumber.isWorkingVersion()) {
//            throw new IllegalStateException("Should not compare against working copy");
//        }
    return BzrContentRevision.createBzrContentRevision(project, vcsRoot, file, hgRevisionNumber);
  }
}
