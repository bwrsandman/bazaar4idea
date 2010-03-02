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
package org.emergent.bzr4j.intellij.provider.update;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.BzrUtil;
import org.emergent.bzr4j.intellij.BzrVcs;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.emergent.bzr4j.intellij.command.BzrUpdateCommand;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class BzrRegularUpdater implements BzrUpdater {

  private static final Logger LOG = Logger.getInstance(BzrRegularUpdater.class.getName());

  private final Project project;
  private final VirtualFile repository;

  public BzrRegularUpdater(Project project, VirtualFile repository) {
    this.project = project;
    this.repository = repository;
  }

  public void update(UpdatedFiles updatedFiles, ProgressIndicator indicator) throws VcsException {
    indicator.setText(BzrVcsMessages.message("bzr4intellij.progress.updating", repository.getPath() ));

//        BzrShowConfigCommand configCommand = new BzrShowConfigCommand(project);
//        String defaultPath = configCommand.getDefaultPath(repository);
//
//        if (StringUtils.isBlank(defaultPath)) {
//            Object[] params = new Object[] { repository.getPath() };
//            VcsException e = new VcsException(
//                    BzrVcsMessages.message("bzr4intellij.warning.no-default-update-path", params)
//            );
//            e.setIsWarning(true);
//            throw e;
//        }

    BzrStandardResult result = pull(repository, indicator);
    List<String> lines = result.getStdErrAsLines();
    Map<String, String> changes = new TreeMap<String, String>();
    BzrRevisionNumber revno = null;
    for (String line : lines) {
      String path = null;
      String fileGroupId = null;
      if (line.startsWith("Text conflict in ")) {
        path = line.substring("Text conflict in ".length());
        fileGroupId = FileGroup.MERGED_WITH_CONFLICT_ID;
        changes.put(path,fileGroupId);
      } else if (line.startsWith("Updated to revision ")) {
        try {
          String revstr = line.substring("Updated to revision ".length(), line.lastIndexOf('.'));
          int revNoInt = Integer.parseInt(revstr);
          revno = BzrRevisionNumber.getLocalInstance(revstr);
          LOG.debug("update revno: " + revno);
        } catch (Exception ignored) {
        }
      } else if (line.length() > 4 && line.charAt(3) == ' ') {
        String flags = line.substring(0, 3);
        if (flags.contains("M")) {
          path = line.substring(4);
//          fileGroupId = FileGroup.MODIFIED_ID;
          fileGroupId = FileGroup.UPDATED_ID;
//          fileGroupId = FileGroup.MERGED_ID;
          if (!changes.containsKey(path))
            changes.put(path,fileGroupId);
        } else if (flags.contains("D")) {
          path = line.substring(4);
          fileGroupId = FileGroup.REMOVED_FROM_REPOSITORY_ID;
//          fileGroupId = FileGroup.LOCALLY_REMOVED_ID;
          if (!changes.containsKey(path))
            changes.put(path,fileGroupId);
        } else if (flags.contains("N")) {
          path = line.substring(4);
          fileGroupId = FileGroup.CREATED_ID;
//          fileGroupId = FileGroup.LOCALLY_ADDED_ID;
          if (!changes.containsKey(path))
            changes.put(path,fileGroupId);
        }
      }
    }

    final VirtualFile bzrRoot = BzrUtil.bzrRootOrNull(repository);
    if (bzrRoot != null) {
      for (Map.Entry<String,String> entry : changes.entrySet()) {
        String relPath = entry.getKey();
        String filePath = (new File(bzrRoot.getPath(), relPath)).getAbsolutePath();
        String fileGroupId = entry.getValue();
        LOG.debug("adding updatedFile: " + fileGroupId + " \"" + relPath + "\"");
        updatedFiles.getGroupById(fileGroupId).add(filePath, BzrVcs.VCS_NAME, revno);
      }
    }


//        String currentBranch = new BzrTagBranchCommand(project, repository).getCurrentBranch();
//        if (StringUtils.isBlank(currentBranch)) {
//            Object[] params = new Object[] { };
//            throw new VcsException(
//                    BzrVcsMessages.message("bzr4intellij.update.error.currentBranch", params)
//            );
//        }

//        //count heads in repository
//        List<BzrRevisionNumber> heads = new BzrHeadsCommand(project, repository).execute(currentBranch);
//        Object[] params1 = new Object[] { };
//        indicator.setText2(BzrVcsMessages.message("bzr4intellij.progress.countingHeads", params1));
//        if (heads.size() < 2) {
//            return;
//        }

//        if (heads.size() > 2) {
//            Object[] params = new Object[] { heads.size() };
//            throw new VcsException(
//                    BzrVcsMessages.message("bzr4intellij.update.error.manyHeads", params)
//            );
//        }
//
//        new BzrHeadMerger(project, new BzrMergeCommand(project, repository))
//                .merge(repository, updatedFiles, indicator, heads.get(heads.size() - 1));
//    new BzrHeadMerger(project, null).merge(repository, updatedFiles, indicator, null);
  }

  private BzrStandardResult pull(VirtualFile repo, ProgressIndicator indicator) throws VcsException {
    indicator.setText2(BzrVcsMessages.message("bzr4intellij.progress.pull.with.update"));
//        BzrPullCommand command = new BzrPullCommand(project, repo);
    BzrUpdateCommand command = new BzrUpdateCommand(project, repo);
//        command.setSource(new BzrShowConfigCommand(project).getDefaultPath(repo));
//        command.setUpdate(true);
//        command.setRebase(false);
    BzrStandardResult result = command.execute();
//    try {
//      result.validate( null, Arrays.asList("update") );
//    } catch (Exception e) {
//      LOG.debug(e);
//    }
    return result;
  }



}
