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
package org.emergent.bzr4j.intellij.provider.commit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.emergent.bzr4j.intellij.command.BzrAddCommand;
import org.emergent.bzr4j.intellij.command.BzrCommandException;
import org.emergent.bzr4j.intellij.command.BzrCommitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BzrCheckinEnvironment implements CheckinEnvironment {

  private final Project project;

  public BzrCheckinEnvironment(Project project) {
    this.project = project;
  }

  public RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel) {
    return createAdditionalOptionsPanel(panel, null);
  }

  public RefreshableOnComponent createAdditionalOptionsPanel(
      CheckinProjectPanel checkinProjectPanel,
      PairConsumer<Object, Object> objectObjectPairConsumer) {
    return null;
  }

  public String getDefaultMessageFor(FilePath[] filesToCheckin) {
    return null;
  }

  public String getHelpId() {
    return null;
  }

  public String getCheckinOperationName() {
    Object[] params = new Object[] { };
    return BzrVcsMessages.message("bzr4intellij.commit", params);
  }

  public List<VcsException> commit(List<Change> changes, String preparedComment) {
    //noinspection unchecked
    return commit(changes, preparedComment, NullableFunction.NULL);
  }

  @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
  public List<VcsException> commit(
      List<Change> changes,
      String preparedComment,
      @NotNull NullableFunction<Object, Object> parametersHolder,
      Set<String> feedback) {
    List<VcsException> exceptions = new LinkedList<VcsException>();
    for (Map.Entry<VirtualFile, List<BzrFile>> entry : getFilesByRepository(changes).entrySet()) {
      BzrCommitCommand command = new BzrCommitCommand(project, entry.getKey(), preparedComment);
      command.setFiles(entry.getValue());
      try {
        command.execute();
      } catch (BzrCommandException e) {
        exceptions.add(new VcsException(e));
      }
    }
    return exceptions;
  }

  public List<VcsException> commit(List<Change> changes, String preparedComment, Object parameters) {
    return commit(changes, preparedComment);
  }

  public List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files) {
//        BzrRemoveCommand command = new BzrRemoveCommand(project);
//        for (FilePath filePath : files) {
//            VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
//            if (vcsRoot == null) {
//                continue;
//            }
//            command.execute(new BzrFile(vcsRoot, filePath));
//        }
    return null;
  }

  public List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files) {
    BzrAddCommand command = new BzrAddCommand(project);
    for (VirtualFile file : files) {
      VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
      if (vcsRoot == null) {
        continue;
      }
      command.execute(new BzrFile(vcsRoot, VfsUtil.virtualToIoFile(file)));
    }
    return null;
  }

  public boolean keepChangeListAfterCommit(ChangeList changeList) {
    return false;
  }

  private Map<VirtualFile, List<BzrFile>> getFilesByRepository(List<Change> changes) {
    Map<VirtualFile, List<BzrFile>> result = new HashMap<VirtualFile, List<BzrFile>>();
    for (Change change : changes) {
      ContentRevision afterRevision = change.getAfterRevision();
      ContentRevision beforeRevision = change.getBeforeRevision();

      FilePath filePath = null;
      if (afterRevision != null) {
        filePath = afterRevision.getFile();
      } else if (beforeRevision != null) {
        filePath = beforeRevision.getFile();
      }

      if (filePath == null) {
        continue;
      }

      VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
      if (vcsRoot == null || filePath.isDirectory()) {
        continue;
      }

      List<BzrFile> bzrFiles = result.get(vcsRoot);
      if (bzrFiles == null) {
        bzrFiles = new LinkedList<BzrFile>();
        result.put(vcsRoot, bzrFiles);
      }

      bzrFiles.add(new BzrFile(vcsRoot, filePath));
    }
    return result;
  }

}
