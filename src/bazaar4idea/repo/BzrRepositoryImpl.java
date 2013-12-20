/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package bazaar4idea.repo;

import bazaar4idea.BzrPlatformFacade;
import bazaar4idea.BzrUtil;
import com.intellij.dvcs.repo.RepositoryImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * @author Kirill Likhodedov
 */
public class BzrRepositoryImpl extends RepositoryImpl implements BzrRepository, Disposable {

  @NotNull private final BzrPlatformFacade myPlatformFacade;

  protected BzrRepositoryImpl(@NotNull VirtualFile rootDir, @NotNull BzrPlatformFacade facade, @NotNull Project project,
                              @NotNull Disposable parentDisposable, final boolean light) {
    super(project, rootDir, parentDisposable);
    myPlatformFacade = facade;
  }

  /**
   * Returns the full-functional instance of GitRepository - with UntrackedFilesHolder and GitRepositoryUpdater.
   * This is used for repositories registered in project, and should be optained via {@link BzrRepositoryManager}.
   */
  @NotNull
  public static BzrRepository getFullInstance(@NotNull VirtualFile root, @NotNull Project project,
                                              @NotNull BzrPlatformFacade facade,
                                              @NotNull Disposable parentDisposable) {
    BzrRepositoryImpl repository = new BzrRepositoryImpl(root, facade, project, parentDisposable, false);
    return repository;
  }

  @Override
  public boolean isFresh() {
    return getCurrentRevision() == null;
  }

  public void update() {
  }

  @Override
  public String toLogString() {
    return String.format("GitRepository " + getRoot() + " : TODO");
  }

}
