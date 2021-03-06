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
package bazaar4idea.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Kirill Likhodedov
 */
public interface Bzr {

  @NotNull
  BzrCommandResult init(@NotNull Project project, @NotNull VirtualFile root, @NotNull BzrLineHandlerListener... listeners);

  @NotNull
  Set<VirtualFile> untrackedFiles(@NotNull Project project, @NotNull VirtualFile root,
                                  @Nullable Collection<VirtualFile> files) throws VcsException;

  // relativePaths are guaranteed to fit into command line length limitations.
  @NotNull
  Collection<VirtualFile> untrackedFilesNoChunk(@NotNull Project project, @NotNull VirtualFile root,
                                                @Nullable List<String> relativePaths) throws VcsException;

}
