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

import bazaar4idea.BzrPlatformFacade;
import bazaar4idea.BzrVcs;
import bazaar4idea.config.BzrVcsSettings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdateSessionAdapter;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class BzrUpdateEnvironment implements UpdateEnvironment {

  private static final Logger LOG = Logger.getInstance(BzrUpdateEnvironment.class.getName());

  private final BzrVcs myVcs;
  private final Project myProject;
  private final BzrVcsSettings mySettings;
  private final BzrUpdaterFactory myUpdaterFactory;
  @NotNull private final BzrPlatformFacade myPlatformFacade;

  public BzrUpdateEnvironment(Project project, @NotNull  BzrVcs vcs, BzrVcsSettings settings) {
    myVcs = vcs;
    myProject = project;
    mySettings = settings;
    myPlatformFacade = ServiceManager.getService(project, BzrPlatformFacade.class);
    myUpdaterFactory = new BzrUpdaterFactory(project);
  }

  public void fillGroups(UpdatedFiles updatedFiles) {
  }

  @NotNull
  public UpdateSession updateDirectories(@NotNull FilePath[] contentRoots,
      UpdatedFiles updatedFiles, ProgressIndicator indicator,
      @NotNull Ref<SequentialUpdatesContext> context) {
    List<VcsException> exceptions = new LinkedList<VcsException>();

    for (FilePath contentRoot : contentRoots) {
      if (indicator != null && indicator.isCanceled()) {
        throw new ProcessCanceledException();
      }
      if (indicator != null) {
        indicator.startNonCancelableSection();
      }
      VirtualFile repository = ProjectLevelVcsManager.getInstance(myProject).getVcsRootFor(contentRoot);
//      VirtualFile repository = VcsUtil.getVcsRootFor(myProject,contentRoot);
      if (repository == null) {
        continue;
      }
      try {
        myUpdaterFactory.buildUpdater(repository).update(updatedFiles, indicator);
      } catch (VcsException e) {
        exceptions.add(e);
      }
      if (indicator != null) {
        indicator.finishNonCancelableSection();
      }
    }
    return new UpdateSessionAdapter(exceptions, false);
  }

  public Configurable createConfigurable(Collection<FilePath> files) {
    return null;
  }

  public boolean validateOptions(Collection<FilePath> roots) {
    return true;
  }

}
