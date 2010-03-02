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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdateSessionAdapter;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.emergent.bzr4j.intellij.command.BzrMergeCommand;
import org.emergent.bzr4j.intellij.command.BzrWorkingCopyRevisionsCommand;
import org.emergent.bzr4j.intellij.data.BzrTagBranch;
import org.emergent.bzr4j.intellij.ui.BzrIntegrateDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class BzrIntegrateEnvironment implements UpdateEnvironment {

  private final Project project;
  private BzrIntegrateDialog mergeDialog;

  public BzrIntegrateEnvironment(Project project) {
    this.project = project;
  }

  public void fillGroups(UpdatedFiles updatedFiles) {
  }

  @NotNull
  public UpdateSession updateDirectories(@NotNull FilePath[] contentRoots,
      final UpdatedFiles updatedFiles, ProgressIndicator progressIndicator,
      @NotNull Ref<SequentialUpdatesContext> context) {
    List<VcsException> exceptions = new LinkedList<VcsException>();
    final VirtualFile repo = mergeDialog.getRepository();
    Object[] params1 = new Object[] { repo.getPath() };
    progressIndicator.setText(
        BzrVcsMessages.message("bzr4intellij.progress.integrating", params1)
    );

    BzrMergeCommand mergeCommand = new BzrMergeCommand(project, repo);

    BzrRevisionNumber incomingRevision = null;
    BzrTagBranch branch = mergeDialog.getBranch();
    if (branch != null) {
      mergeCommand.setBranch(branch.getName());
      incomingRevision = branch.getHead();
    }

    BzrTagBranch tag = mergeDialog.getTag();
    if (tag != null) {
      mergeCommand.setRevision(tag.getName());
      incomingRevision = tag.getHead();
    }

    String revision = mergeDialog.getRevision();
    if (revision != null) {
      mergeCommand.setRevision(revision);
      incomingRevision = BzrRevisionNumber.getLocalInstance(revision);
    }

    if (incomingRevision != null) {
      try {
        new BzrHeadMerger(project, mergeCommand).merge(repo, updatedFiles, progressIndicator, incomingRevision);

        final BzrRevisionNumber localRevision =
            new BzrWorkingCopyRevisionsCommand(project).parent(repo);

        final BzrRevisionNumber incomingRevisionFinal = incomingRevision;
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
          public void run() {
            new BzrConflictResolver(project, incomingRevisionFinal, localRevision, updatedFiles)
                .resolve(repo);
          }
        }, ModalityState.defaultModalityState());

      } catch (VcsException e) {
        exceptions.add(e);
      }
    } else {
      Object[] params = new Object[] { };
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(BzrVcsMessages.message("bzr4intellij.error.invalidTarget", params)));
    }

    return new UpdateSessionAdapter(exceptions, false);
  }

  public Configurable createConfigurable(Collection<FilePath> roots) {
    mergeDialog = new BzrIntegrateDialog(project, roots);
    return mergeDialog;
  }

  public boolean validateOptions(Collection<FilePath> roots) {
    return true;
  }

}
