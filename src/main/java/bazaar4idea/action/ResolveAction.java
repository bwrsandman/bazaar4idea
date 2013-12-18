/**
 * @copyright
 * ====================================================================
 * Copyright (c) 2003-2004 QintSoft.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://subversion.tigris.org/license-1.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 *
 * This software consists of voluntary contributions made by many
 * individuals.  For exact contribution history, see the revision
 * history and logs, available at http://svnup.tigris.org/.
 * ====================================================================
 * @endcopyright
 */
/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bazaar4idea.action;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.merge.MergeData;
import com.intellij.openapi.vcs.merge.MergeProvider;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import bazaar4idea.BzrVcs;
import bazaar4idea.BzrVcsMessages;
import bazaar4idea.command.BzrResolveCommand;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResolveAction extends BzrAbstractFilesAction {

  private static final Logger LOG = Logger.getInstance(ResolveAction.class.getName());

  protected boolean isEnabled(Project project, BzrVcs vcs, VirtualFile file) {
    if (file.isDirectory()) return true;
    final FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
    return fileStatus != null && FileStatus.MERGED_WITH_CONFLICTS.equals(fileStatus);
  }

  @Override
  protected void batchPerform(final Project project, final BzrVcs activeVcs, final List<VirtualFile> files,
      DataContext context) throws VcsException {
    boolean hasDirs = false;
    for (VirtualFile file : files) {
      if (file.isDirectory()) {
        hasDirs = true;
      }
    }
    final List<VirtualFile> fileList = new ArrayList<VirtualFile>();
    if (!hasDirs) {
      for (VirtualFile file : files) {
        addIfWritable(file, project, fileList);
      }
    } else {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
        public void run() {
          FileStatusManager statusMgr = FileStatusManager.getInstance(project);
          for (VirtualFile file : files) {
            if (file.isDirectory()) {
              ProjectLevelVcsManager.getInstance(project).iterateVcsRoot(file, new Processor<FilePath>() {
                public boolean process(final FilePath filePath) {
                  ProgressManager.checkCanceled();
                  VirtualFile fileOrDir = filePath.getVirtualFile();
                  if (fileOrDir != null && !fileOrDir.isDirectory()
                      && isEnabled(project, activeVcs, fileOrDir)
                      && !fileList.contains(fileOrDir)) {
//                        addIfWritable(fileOrDir, project, fileList);
                    fileList.add(fileOrDir);
                  }
                  return true;
                }
              });
            } else {
              if (!fileList.contains(file) && FileStatus.MERGED_WITH_CONFLICTS.equals(statusMgr.getStatus(file))) {
                fileList.add(file);
              }
            }
          }
        }
      }, BzrVcsMessages.message("progress.searching.for.files.with.conflicts"), true, project);
    }
    AbstractVcsHelper.getInstance(project).showMergeDialog(fileList, buildMergeProvider(project));
  }

  private void addIfWritable(final VirtualFile fileOrDir, final Project project, final List<VirtualFile> fileList) {
    final ReadonlyStatusHandler.OperationStatus operationStatus =
        ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(fileOrDir);
    if (!operationStatus.hasReadonlyFiles()) {
      fileList.add(fileOrDir);
    }
  }

  private MergeProvider buildMergeProvider(final Project project) {
    return new MergeProvider() {
      @NotNull
      public MergeData loadRevisions(final VirtualFile file) throws VcsException {
        final MergeData data = new MergeData();
        VcsRunnable runnable = new VcsRunnable() {
          public void run() throws VcsException {
            File oldFile = new File(file.getPath() + ".BASE");
            File newFile = new File(file.getPath() + ".OTHER");
            File workingFile = new File(file.getPath() + ".THIS");
            try {
              data.ORIGINAL = FileUtil.loadFileBytes(oldFile);
              data.LAST = FileUtil.loadFileBytes(newFile);
              data.CURRENT = FileUtil.loadFileBytes(workingFile);
            } catch (IOException e) {
              throw new VcsException(e);
            }
          }
        };
        VcsUtil.runVcsProcessWithProgress(runnable,
            VcsBundle.message("multiple.file.merge.loading.progress.title"), false, project);

        return data;
      }

      public void conflictResolvedForFile(VirtualFile file) {
        BzrResolveCommand resolveCommand = new BzrResolveCommand(project);
        VirtualFile root = VcsUtil.getVcsRootFor(project, file);
        if (root == null) {
          return;
        }
        resolveCommand.resolve(root, file);
        final VirtualFile parent = file.getParent();
        if (parent != null) {
          parent.refresh(true, false);
        }
      }

      public boolean isBinary(final VirtualFile file) {
        try {
          byte[] bytes = FileUtil.loadFileBytes(new File(file.getPath()));
          for (byte aByte : bytes) {
            if (aByte == 0)
              return true;
          }
        } catch (IOException ignored) {
        }
        return false;
      }

    };
  }
}
