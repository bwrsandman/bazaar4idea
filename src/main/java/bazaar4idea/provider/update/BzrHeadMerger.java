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
package bazaar4idea.provider.update;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.cli.BzrExecException;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.xmloutput.GenericChange;
import org.emergent.bzr4j.core.xmloutput.XmlOutputHandler;
import bazaar4idea.BzrVcs;
import bazaar4idea.BzrVcsMessages;
import bazaar4idea.command.BzrIdeaExec;
import bazaar4idea.command.BzrMergeCommand;
import bazaar4idea.command.ShellCommandService;

import java.io.File;

final class BzrHeadMerger {

  private final Project m_project;
  private final BzrMergeCommand hgMergeCommand;

  public BzrHeadMerger(Project project, BzrMergeCommand hgMergeCommand) {
    this.m_project = project;
    this.hgMergeCommand = hgMergeCommand;
  }

  public void merge(VirtualFile repo, final UpdatedFiles updatedFiles, ProgressIndicator indicator, final VcsRevisionNumber vcsRevNo)
      throws VcsException {
    indicator.setText2(BzrVcsMessages.message("bzr4intellij.progress.merging"));

//    hgMergeCommand.execute();

    VcsDirtyScopeManager vcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(m_project);
    vcsDirtyScopeManager.dirDirtyRecursively(repo);
    repo.refresh(false, true);

//    BzrStatusCommand hgStatusCommand = new BzrStatusCommand(project);
//    hgStatusCommand.setIncludeIgnored(false);
//    hgStatusCommand.setIncludeUnknown(false);
//    Set<BzrChange> changes = null; // hgStatusCommand.execute(repo);

    try {
      collectChanges(new XmlOutputHandler() {

        @Override
        public void handleGenericChange(GenericChange change) {
//          String path = change.m_path;
//          Attributes attributes = change.m_attributes;
          String fileGroupId = null;
          File workDir = getWorkDir();

          switch (change.m_changeType) {
            case added:
              fileGroupId = FileGroup.LOCALLY_ADDED_ID;
//              handleAdded(change.m_kind, change.m_path);
              break;
            case modified:
              fileGroupId = FileGroup.MODIFIED_ID;
//              handleModified(change.m_kind, change.m_path);
              break;
            case removed:
              fileGroupId = FileGroup.LOCALLY_REMOVED_ID;
//              handleRemoved(change.m_kind, change.m_path);
              break;
            case renamed:
              fileGroupId = FileGroup.MODIFIED_ID;
//              handleRenamed(change.m_kind, change.m_path, change.m_attributes.getValue("oldpath"));
              break;
            case unknown:
//              handleUnknown(change.m_kind, change.m_path);
              break;
            case conflicts:
              fileGroupId = FileGroup.MERGED_WITH_CONFLICT_ID;
//              handleConflicts(change.m_path, change.m_attributes.getValue("type"));
              break;
            case kind_changed:
              fileGroupId = FileGroup.MODIFIED_ID;
//              handleKindChanged(change.m_kind, change.m_path, change.m_attributes.getValue("oldkind"));
              break;
          }

          if (fileGroupId != null) {
            String filePath = (new File(workDir, change.m_path)).getAbsolutePath();
//            LOG.debug("adding updatedFile: " + fileGroupId + " " + filePath);
            updatedFiles.getGroupById(fileGroupId).add(filePath, BzrVcs.VCS_NAME, vcsRevNo);
          }
        }
      }, repo);
    } catch (BzrExecException e) {
      throw new VcsException(e);
    }
//
//    if (changes.isEmpty()) {
//      return;
//    }
//
//    for (BzrChange change : changes) {
//      BzrFile afterFile = change.afterFile();
//      BzrFile beforeFile = change.beforeFile();
//      String fileGroupId = null;
//      String filePath = null;
//      if (afterFile != null && beforeFile != null) {
//        fileGroupId = FileGroup.MODIFIED_ID;
//        filePath = afterFile.getFile().getAbsolutePath();
//      } else if (beforeFile != null) {
//        fileGroupId = FileGroup.LOCALLY_REMOVED_ID;
//        filePath = beforeFile.getFile().getAbsolutePath();
//      } else if (afterFile != null) {
//        fileGroupId = FileGroup.LOCALLY_ADDED_ID;
//        filePath = afterFile.getFile().getAbsolutePath();
//      }
//      if (fileGroupId != null && filePath != null) {
//        updatedFiles.getGroupById(fileGroupId).add(filePath, BzrVcs.VCS_NAME, vcsRevNo);
//      }
//    }
  }


  private void collectChanges(XmlOutputHandler statusHandler, VirtualFile filePath) throws BzrExecException {
//    BzrIdeaExec.createBzrIdeaExec(filePath, "xmlstatus").execij(statusHandler);
    ShellCommandService cmdSvc = ShellCommandService.getInstance(m_project);
    cmdSvc.execute(BzrIdeaExec.createBzrIdeaExec(filePath, "xmlstatus"), BzrXmlResult.createBzrXmlResult(statusHandler));
  }

}
