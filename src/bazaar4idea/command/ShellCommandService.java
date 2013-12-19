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
package bazaar4idea.command;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsImplUtil;
import org.emergent.bzr4j.core.cli.BzrAbstractResult;
import org.emergent.bzr4j.core.cli.BzrExecService;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.xmloutput.XmlPluginHandler;
import bazaar4idea.BzrGlobalSettings;
import bazaar4idea.BzrVcsMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ShellCommandService extends BzrExecService<BzrIdeaExec> {

  private static final Logger LOG = Logger.getInstance(ShellCommandService.class.getName());

  private static final ConcurrentMap<String, Lock> sm_workDirLocks = new ConcurrentHashMap<String, Lock>();

  private final Project project;
  private final BzrGlobalSettings settings;

  public ShellCommandService(Project project, BzrGlobalSettings settings) {
    this.project = project;
    this.settings = settings;
  }

  public static ShellCommandService getInstance(Project project) {
    return project.getComponent(ShellCommandService.class);
  }

  public static boolean isValid(final String executable) {
    try {
      BzrIdeaExec shellCommand = new BzrIdeaExec("version") {
        @Override
        protected String getBzrExecutablePath() {
          return executable;
        }
      };
      shellCommand.setExitValueValidationEnabled(true);
      shellCommand.setStderrValidationEnabled(true);
      shellCommand.exec(new BzrStandardResult());
      return true;
    } catch (Exception e) {
//      BzrAbstractCommand.LOG.error(e.getMessage(), e);
      return false;
    }
  }

  public static boolean isValidPlugins(XmlPluginHandler handler) {
    try {
      BzrIdeaExec shellCommand = new BzrIdeaExec("xmlplugins");
      shellCommand.setExitValueValidationEnabled(true);
      shellCommand.setStderrValidationEnabled(true);
      shellCommand.exec(BzrXmlResult.createBzrXmlResult(handler));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Nullable
  public BzrIdeaExec createCommand(@NotNull VirtualFile repo, @NotNull String cmd) {
    return BzrIdeaExec.createBzrIdeaExec(repo, cmd);
  }

  BzrStandardResult execute2(@NotNull VirtualFile repo, String op, List<String> args) {
    BzrIdeaExec shellCommand = createCommand(repo, op);
    shellCommand.addArguments(args);
    shellCommand.setBad(true);
    return execute(shellCommand);
  }

  BzrStandardResult execute(@NotNull VirtualFile repo, String op, List<String> args) {
    BzrIdeaExec shellCommand = createCommand(repo, op);
    shellCommand.addArguments(args);
    return execute(shellCommand);
  }

  BzrStandardResult execute(@NotNull VirtualFile repo, BzrStandardResult result, String op, List<String> args) {
    BzrIdeaExec shellCommand = createCommand(repo, op);
    shellCommand.addArguments(args);
    return execute(shellCommand, result);
  }

  BzrStandardResult execute(@NotNull VirtualFile repo, String op, String... args) {
    BzrIdeaExec shellCommand = createCommand(repo, op);
    shellCommand.addArguments(args);
    return execute(shellCommand);
  }

  public BzrStandardResult execute(BzrIdeaExec shellCmd) {
    return execute(shellCmd, new BzrStandardResult(), BzrStandardResult.EMPTY);
  }

  @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
  public <T extends BzrAbstractResult> T execute(BzrIdeaExec shellCmd, T result, T defaultResult) {
    if (shellCmd.isBad()) {
      Exception e = new Exception("badcmd: " + shellCmd.getCmd());
      LOG.debug(e);
    } else {
      return super.execute(shellCmd, result, defaultResult);
    }
    return defaultResult;
  }

  protected void showError(Throwable e) {
    LOG.error("BzrExecException", e);
    if (!BzrGlobalSettings.getInstance().isModalErrorPopupEnabled())
      return;

    StringBuilder message = new StringBuilder();
    Object[] params1 = new Object[] { settings.getBzrExecutable() };
    message.append(BzrVcsMessages.message("bzr4intellij.command.executable.error", params1))
        .append("\n")
        .append("Original Error:\n")
        .append(e.getMessage());

    Object[] params = new Object[] { };
    VcsImplUtil.showErrorMessage(
        project,
        message.toString(),
        BzrVcsMessages.message("bzr4intellij.error", params)
    );
  }

  protected static void lockWorkDir(File lockDir) {
    String path = lockDir == null ? "." : lockDir.getAbsolutePath();
    LOG.debug(String.format("Locking   \"%s\"", path));
    getWorkDirLock(path).lock();
  }

  protected static void unlockWorkDir(File lockDir) {
    String path = lockDir == null ? "." : lockDir.getAbsolutePath();
    LOG.debug(String.format("Unlocking \"%s\"", path));
    getWorkDirLock(path).unlock();
  }

  private static Lock getWorkDirLock(String path) {
//    String path = workDir != null ? workDir.getAbsolutePath() : ".";
    Lock lock = sm_workDirLocks.get(path);
    if (lock == null) {
      lock = new ReentrantLock();
      Lock curLock = sm_workDirLocks.putIfAbsent(path, lock);
      if (curLock != null) {
        lock = curLock;
      }
    }
    return lock;
  }
}
