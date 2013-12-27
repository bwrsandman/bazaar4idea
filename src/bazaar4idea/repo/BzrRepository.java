/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface BzrRepository extends Repository {

  @NotNull
  VirtualFile getBzrDir();

  @NotNull
  BzrUntrackedFilesHolder getUntrackedFilesHolder();

  /**
   * Returns remotes defined in this Bazaar repository.
   * It is different from {@link BzrConfig#getRemotes()} because remotes may be defined not only in {@code .git/config},
   * but in {@code .git/remotes/} or even {@code .git/branches} as well.
   * On the other hand, it is a very old way to define remotes and we are not going to implement this until needed.
   * See <a href="http://thread.gmane.org/gmane.comp.version-control.git/182960">discussion in the Git mailing list</a> that confirms
   * that remotes a defined in {@code .git/config} only nowadays.
   * @return GitRemotes defined for this repository.
   */
  @NotNull
  Collection<BzrRemote> getRemotes();

}
