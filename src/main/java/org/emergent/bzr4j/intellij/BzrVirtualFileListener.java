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
package org.emergent.bzr4j.intellij;

import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import static com.intellij.openapi.vcs.VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
import static com.intellij.openapi.vcs.VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.LocalFileOperationsHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.util.ThrowableConsumer;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.intellij.command.BzrAddCommand;
import org.emergent.bzr4j.intellij.command.BzrFileIdCommand;
import org.emergent.bzr4j.intellij.command.BzrMiscCommand;
import org.emergent.bzr4j.intellij.command.BzrMoveCommand;
import org.emergent.bzr4j.intellij.command.BzrRemoveCommand;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class BzrVirtualFileListener extends VirtualFileAdapter
    implements LocalFileOperationsHandler, CommandListener {

  private final Project project;
  private final AbstractVcs vcs;

  public BzrVirtualFileListener(Project project, AbstractVcs vcs) {
    this.project = project;
    this.vcs = vcs;
  }

//  @Override
//  public void fileCopied(VirtualFileCopyEvent event) {
//    if (event.isFromRefresh()) {
//      return;
//    }
//    final VirtualFile file = event.getFile();
//    if (!VcsUtil.isFileForVcs(file, project, vcs)) {
//      return;
//    }
//    if (!isFileProcessable(file)) {
//      return;
//    }
//
//    VirtualFile newFile = event.getFile();
//    VirtualFile oldFile = event.getOriginalFile();
//    VirtualFile repo = VcsUtil.getVcsRootFor(project, file);
//    if (repo == null) {
//      return;
//    }
//
//    HgCopyCommand command = new HgCopyCommand(project);
//    BzrFile source = new BzrFile(repo, new File(oldFile.getPath()));
//    BzrFile target = new BzrFile(repo, new File(newFile.getPath()));
//    command.execute(source, target);
//  }

  @Override
  public void fileCreated(VirtualFileEvent event) {
    if (event.isFromRefresh()) {
      return;
    }
    final VirtualFile file = event.getFile();
    if (!VcsUtil.isFileForVcs(file, project, vcs)) {
      return;
    }
    if (!isFileProcessable(file)) {
      return;
    }

    String title = BzrVcsMessages.message("bzr4intellij.add.confirmation.title");
    Object[] params = new Object[] { file.getPath() };
    String message = BzrVcsMessages.message("bzr4intellij.add.confirmation.body", params);

    ProjectLevelVcsManager vcsMgr = ProjectLevelVcsManager.getInstance(project);

    VcsShowConfirmationOption option =
        vcsMgr.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, vcs);

    boolean processAdd = false;
    if (DO_ACTION_SILENTLY == option.getValue()) {
      processAdd = true;
    } else if (SHOW_CONFIRMATION == option.getValue()) {
      AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
      processAdd = null != helper.selectFilesToProcess(
          Arrays.asList(file), title, null, title, message, option
      );
    }
    VirtualFile repo = VcsUtil.getVcsRootFor(project, file);
    if (processAdd && repo != null) {
      new BzrAddCommand(project).execute(new BzrFile(repo, VfsUtil.virtualToIoFile(file)));
    }
  }

  @Override
  public void fileDeleted(VirtualFileEvent event) {
    if (event.isFromRefresh()) {
      return;
    }

    final VirtualFile file = event.getFile();
    if (!VcsUtil.isFileForVcs(file, project, vcs)) {
      return;
    }

    if (!isFileProcessable(file)) {
      return;
    }

    FileStatus status = FileStatusManager.getInstance(project).getStatus(file);
    if (status == FileStatus.UNKNOWN || status == FileStatus.IGNORED) {
      return;
    }

    String title = BzrVcsMessages.message("bzr4intellij.delete.confirmation.title");
    String message = BzrVcsMessages.message("bzr4intellij.delete.confirmation.body", file.getPath());

    VcsShowConfirmationOption option = ProjectLevelVcsManager.getInstance(project)
        .getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, vcs);

    boolean processDelete = false;
    if (DO_ACTION_SILENTLY == option.getValue()) {
      processDelete = true;
    } else if (SHOW_CONFIRMATION == option.getValue()) {
      AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
      processDelete = null != helper.selectFilesToProcess(
          Arrays.asList(file), title, null, title, message, option
      );
    }
    VirtualFile repo = VcsUtil.getVcsRootFor(project, file);
    if (processDelete && repo != null) {
      new BzrRemoveCommand(project).execute(new BzrFile(repo, new File(file.getPath())));
    }
  }

  @Override
  public void fileMoved(VirtualFileMoveEvent event) {
    VirtualFile newParent = event.getNewParent();
    VirtualFile oldParent = event.getOldParent();
    String fileName = event.getFileName();
    VirtualFile repo = VcsUtil.getVcsRootFor(project, event.getFile());
    if (repo == null) {
      return;
    }
    BzrFile source = new BzrFile(repo, new File(oldParent.getPath(), fileName));
    if (!isVersioned(source) /* && isVersioned(newParent) */) // todo we should offer to add the parent if unversioned
      return;
    BzrFile target = new BzrFile(repo, new File(newParent.getPath(), fileName));
    BzrMoveCommand command = new BzrMoveCommand(project);
    command.execute(source, target);
  }

  @Override
  public void propertyChanged(VirtualFilePropertyEvent event) {
    if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
      fileRenamed(event);
    }
  }

  private void fileRenamed(VirtualFilePropertyEvent event) {
    VirtualFile file = event.getFile();
    VirtualFile parent = file.getParent();
    String oldName = (String)event.getOldValue();
    String newName = (String)event.getNewValue();
    VirtualFile repo = VcsUtil.getVcsRootFor(project, file);
    if (repo == null || parent == null) {
      return;
    }
    BzrMoveCommand command = new BzrMoveCommand(project);
    BzrFile source = new BzrFile(repo, new File(parent.getPath(), oldName));
    if (!isVersioned(source))
      return;
    BzrFile target = new BzrFile(repo, new File(parent.getPath(), newName));
    command.execute(source, target);
  }

  private boolean isVersioned(VirtualFile file) {
    VirtualFile repo = VcsUtil.getVcsRootFor(project, file);
    if (repo == null) {
      return false;
    }
    return isVersioned(new BzrFile(repo, new File(file.getPath())));
  }

  private boolean isVersioned(BzrFile file) {
    return (new BzrFileIdCommand(project)).isVersioned(file);
  }

  private boolean isFileProcessable(VirtualFile file) {
    if (file == null) {
      return false;
    }
    if (FileTypeManager.getInstance().isFileIgnored(file.getName()))
      return false;
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    if (changeListManager.isIgnoredFile(file))
      return false;
    if (BzrMiscCommand.isIgnored(project, file))
      return false;
    return true;
  }

  public void afterDone(ThrowableConsumer<LocalFileOperationsHandler, IOException> invoker) {
  }

  public File copy(VirtualFile file, VirtualFile toDir, String copyName) throws IOException {
    return null;
  }

  public boolean createDirectory(VirtualFile dir, String name) throws IOException {
    return false;
  }

  public boolean createFile(VirtualFile dir, String name) throws IOException {
    return false;
  }

  public boolean delete(VirtualFile file) throws IOException {
    return false;
  }

  public boolean move(VirtualFile file, VirtualFile toDir) throws IOException {
    return false;
  }

  public boolean rename(VirtualFile file, String newName) throws IOException {
    return false;
  }

  public void beforeCommandFinished(CommandEvent event) {
  }

  public void commandStarted(CommandEvent event) {
  }

  public void commandFinished(CommandEvent event) {
  }

  public void undoTransparentActionStarted() {
  }

  public void undoTransparentActionFinished() {
  }
}
