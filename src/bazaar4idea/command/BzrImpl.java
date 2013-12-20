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
package bazaar4idea.command;

import bazaar4idea.command.Bzr;
import bazaar4idea.command.BzrCommandResult;
import bazaar4idea.command.BzrLineHandlerListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Easy-to-use wrapper of common native Bzr commands.
 * Most of them return result as {@link BzrCommandResult}.
 *
 * @author Kirill Likhodedov
 */
public class BzrImpl implements Bzr {

  private final Logger LOG = Logger.getInstance(Bzr.class);

  public BzrImpl() {
  }

  /**
   * Runs the given {@link BzrLineHandler} in the current thread and returns the {@link BzrCommandResult}.
   */
  private static BzrCommandResult run(@NotNull BzrLineHandler handler) {
    final List<String> errorOutput = new ArrayList<String>();
    final List<String> output = new ArrayList<String>();
    final AtomicInteger exitCode = new AtomicInteger();
    final AtomicBoolean startFailed = new AtomicBoolean();
    final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();

    handler.addLineListener(new BzrLineHandlerListener() {
      @Override public void onLineAvailable(String line, Key outputType) {
        if (isError(line)) {
          errorOutput.add(line);
        } else {
          output.add(line);
        }
      }

      @Override public void processTerminated(int code) {
        exitCode.set(code);
      }

      @Override public void startFailed(Throwable t) {
        startFailed.set(true);
        errorOutput.add("Failed to start Git process");
        exception.set(t);
      }
    });

    handler.runInCurrentThread(null);

    if (handler instanceof BzrLineHandlerPasswordRequestAware &&
        ((BzrLineHandlerPasswordRequestAware)handler).hadAuthRequest()) {
      errorOutput.add("Authentication failed");
    }

    final boolean success = !startFailed.get() && errorOutput.isEmpty() &&
            (handler.isIgnoredErrorCode(exitCode.get()) || exitCode.get() == 0);
    return new BzrCommandResult(success, exitCode.get(), errorOutput, output, null);
  }

  /**
   * <p>Queries Git for the unversioned files in the given paths. </p>
   * <p>Ignored files are left ignored, i. e. no information is returned about them (thus this method may also be used as a
   *    ignored files checker.</p>
   *
   * @param files files that are to be checked for the unversioned files among them.
   *              <b>Pass <code>null</code> to query the whole repository.</b>
   * @return Unversioned not ignored files from the given scope.
   */
  @Override
  @NotNull
  public Set<VirtualFile> untrackedFiles(@NotNull Project project, @NotNull VirtualFile root,
                                         @Nullable Collection<VirtualFile> files) throws VcsException {
    final Set<VirtualFile> untrackedFiles = new HashSet<VirtualFile>();

    if (files == null) {
      untrackedFiles.addAll(untrackedFilesNoChunk(project, root, null));
    }
    else {
      for (List<String> relativePaths : VcsFileUtil.chunkFiles(root, files)) {
        untrackedFiles.addAll(untrackedFilesNoChunk(project, root, relativePaths));
      }
    }

    return untrackedFiles;
  }

  // relativePaths are guaranteed to fit into command line length limitations.
  @Override
  @NotNull
  public Collection<VirtualFile> untrackedFilesNoChunk(@NotNull Project project,
                                                       @NotNull VirtualFile root,
                                                       @Nullable List<String> relativePaths)
          throws VcsException {
    final Set<VirtualFile> untrackedFiles = new HashSet<VirtualFile>();
    BzrSimpleHandler h = new BzrSimpleHandler(project, root, BzrCommand.LS_FILES);
    h.setSilent(true);
    h.addParameters("--unknown");
    h.endOptions();
    if (relativePaths != null) {
      h.addParameters(relativePaths);
    }

    final String output = h.run();
    if (StringUtil.isEmptyOrSpaces(output)) {
      return untrackedFiles;
    }

    for (String relPath : output.split("\u0000")) {
      VirtualFile f = root.findFileByRelativePath(relPath);
      if (f == null) {
        // files was created on disk, but VirtualFile hasn't yet been created,
        // when the GitChangeProvider has already been requested about changes.
        LOG.info(String.format("VirtualFile for path [%s] is null", relPath));
      } else {
        untrackedFiles.add(f);
      }
    }

    return untrackedFiles;
  }

  /**
   * Check if the line looks line an error message
   */
  private static boolean isError(String text) {
    for (String indicator : ERROR_INDICATORS) {
      if (text.startsWith(indicator.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  // could be upper-cased, so should check case-insensitively
  public static final String[] ERROR_INDICATORS = {
          "error", "remote: error", "fatal",
          "Cannot apply", "Could not", "Interactive rebase already started", "refusing to pull", "cannot rebase:", "conflict",
          "unable"
  };

  /**
   * Calls 'bzr init' on the specified directory.
   */
  @NotNull
  @Override
  public BzrCommandResult init(@NotNull Project project, @NotNull VirtualFile root, @NotNull BzrLineHandlerListener... listeners) {
    BzrLineHandler h = new BzrLineHandler(project, root, BzrCommand.INIT);
    for (BzrLineHandlerListener listener : listeners) {
      h.addLineListener(listener);
    }
    h.setSilent(false);
    return run(h);
  }


}
