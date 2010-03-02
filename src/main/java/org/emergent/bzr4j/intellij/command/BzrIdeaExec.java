/*
 * Copyright (c) 2010 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.emergent.bzr4j.intellij.command;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.BazaarRoot;
import org.emergent.bzr4j.core.cli.BzrAbstractExec;
import org.emergent.bzr4j.core.cli.BzrAbstractResult;
import org.emergent.bzr4j.core.cli.BzrExecException;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.utils.BzrCoreUtil;
import org.emergent.bzr4j.core.utils.FileUtil;
import org.emergent.bzr4j.intellij.BzrGlobalSettings;
import org.emergent.bzr4j.intellij.BzrUtil;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Patrick Woodworth
 */
public class BzrIdeaExec extends BzrAbstractExec {

  private static final Logger LOG = Logger.getInstance(BzrIdeaExec.class.getName());

  private static final ConcurrentMap<String, Lock> sm_workDirLocks = new ConcurrentHashMap<String, Lock>();

  private boolean m_bad;

  public BzrIdeaExec(String cmd) {
    this(BazaarRoot.ROOTLESS, cmd);
  }

  public BzrIdeaExec(BazaarRoot root, String cmd) {
    super(root, cmd);
  }

  @Nullable
  public static BzrIdeaExec createBzrIdeaExec(VirtualFile repo, String cmd) {
    BazaarRoot bzrRoot = BazaarRoot.findBranchLocation(VfsUtil.virtualToIoFile(repo));
    if (bzrRoot == null)
      return null;
    return new BzrIdeaExec(bzrRoot, cmd);
  }

  @Override
  protected BzrAbstractResult exec(BzrAbstractResult result) throws BzrExecException {
    final Lock lock = getWorkDirLock(getWorkingDir());
    lock.lock();
    try {
      return super.exec(result);
    } finally {
      lock.unlock();
    }
  }

  public void addRelativePaths(FilePath... paths) {
    for (FilePath path : paths) {
      addArguments(BzrUtil.relativePath(getWorkingDir(),path));
    }
  }

  public void addRelativePaths(VirtualFile... files) {
    for (VirtualFile file : files) {
      addArguments(BzrUtil.relativePath(getWorkingDir(),file));
    }
  }

  public boolean isBad() {
    return m_bad;
  }

  public void setBad(boolean bad) {
    m_bad = bad;
  }

  @Override
  protected ProcessBuilder createProcessBuilder(List<String> args) {
    ProcessBuilder retval = super.createProcessBuilder(args);
    Map<String, String> envVars = retval.environment();
    for (Map.Entry<String,String> override : BzrGlobalSettings.getInstance().getEnvironmentVariables().entrySet()) {
      String key = override.getKey();
      String value = override.getValue();
      if (value != null && value.trim().length() > 0) {
        envVars.put(key, value);
      } else {
        envVars.remove(key);
      }
    }
    return retval;
  }

  @Override
  protected String getBzrExecutablePath() {
    return BzrGlobalSettings.getInstance().getBzrExecutable();
  }

  public static Lock getWorkDirLock(File workDir) {
    boolean fineGrain = BzrGlobalSettings.getInstance().isGranularExecLockingEnabled();
    String path = (fineGrain && (workDir != null)) ? workDir.getAbsolutePath() : ".";
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
