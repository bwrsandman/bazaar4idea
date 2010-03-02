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
package org.emergent.bzr4j.intellij.provider;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.core.BazaarRoot;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.xmloutput.XmlOutputHandler;
import org.emergent.bzr4j.intellij.BzrContentRevision;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.BzrUtil;
import org.emergent.bzr4j.intellij.command.BzrIdeaExec;
import org.emergent.bzr4j.intellij.command.BzrMiscCommand;
import org.emergent.bzr4j.intellij.command.ShellCommandService;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BzrChangeProvider implements ChangeProvider {

  private static final Logger LOG = Logger.getInstance(BzrChangeProvider.class.getName());

  private static final Logger CHANGES = Logger.getInstance(BzrChangeProvider.class.getName() + ".CHANGES");

  private static final Logger IGNORED = Logger.getInstance(BzrChangeProvider.class.getName() + ".IGNORED");

  private static final Logger UNKNOWN = Logger.getInstance(BzrChangeProvider.class.getName() + ".UNKNOWN");

  private final Project m_project;
  private final VcsKey m_vcsKey;

  public BzrChangeProvider(Project project, VcsKey vcsKey) {
    this.m_project = project;
    this.m_vcsKey = vcsKey;
  }

  public void getChanges(
      VcsDirtyScope dirtyScope,
      ChangelistBuilder builder,
      ProgressIndicator progress,
      ChangeListManagerGate addGate) throws VcsException {

    VcsContextFactory vcsCtxFac = VcsContextFactory.SERVICE.getInstance();

    Map<File,BzrRevisionNumber> rootRevnos = new LinkedHashMap<File,BzrRevisionNumber>();

    Map<VirtualFile,FilePath> rootsMap = new HashMap<VirtualFile, FilePath>();

    for (FilePath filePath : dirtyScope.getRecursivelyDirtyDirectories()) {
      mergePaths(vcsCtxFac, rootsMap, filePath);
    }

    for (FilePath filePath : dirtyScope.getDirtyFiles()) {
      mergePaths(vcsCtxFac, rootsMap, filePath);
    }

    for (Map.Entry<VirtualFile,FilePath> rootEntry : rootsMap.entrySet()) {
      process(builder, rootEntry.getKey(), rootEntry.getValue(), rootRevnos);
    }
  }

  private void mergePaths(VcsContextFactory vcsCtxFac, Map<VirtualFile, FilePath> rootsMap, FilePath filePath)
      throws VcsException {

    VirtualFile vcsVirtualRoot = VcsUtil.getVcsRootFor(m_project, filePath);
    if (vcsVirtualRoot == null) {
      CHANGES.debug("no processing (no virtual root): " + String.valueOf(filePath));
      return;
    }
    if (rootsMap.containsKey(vcsVirtualRoot)) {
      CHANGES.debug("no processing (duplicate root): " + String.valueOf(filePath));
      return;
    }
    FilePath vcsPathRoot = vcsCtxFac.createFilePathOn(vcsVirtualRoot);
    if (vcsPathRoot == null) {
      CHANGES.debug("no processing (no path root): " + String.valueOf(filePath));
      return;
    }
    rootsMap.put(vcsVirtualRoot,vcsPathRoot);
  }


  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }

  public void doCleanup(List<VirtualFile> files) {
  }

  private void process(
      ChangelistBuilder builder,
      VirtualFile vcsVirtualRoot,
      FilePath filePath,
      Map<File, BzrRevisionNumber> processedRoots) {

    if (filePath.isNonLocal()) {
      CHANGES.debug("no processing (nonlocal path): " + String.valueOf(filePath));
      return;
    }


    Set<File> ignoredSet = new TreeSet<File>();
    final File bzrRoot = collectIgnored(ignoredSet, filePath);
    if (bzrRoot == null) {
      CHANGES.debug("no processing (no io root): " + String.valueOf(filePath));
      return;
    }

    CHANGES.debug("is processing: " + String.valueOf(filePath));

    for (File ignored : ignoredSet) {
      IGNORED.debug(String.format("%10s \"%s\"", "ignored", ignored));
        builder.processIgnoredFile(VcsUtil.getVirtualFile(ignored));
    }

    BzrRevisionNumber revno = processedRoots.get(bzrRoot);
    if (revno == null) {
      revno = BzrMiscCommand.revno(m_project,vcsVirtualRoot);
      processedRoots.put(bzrRoot,revno);
    }
    MyStatusHandler statusHandler = new MyStatusHandler(vcsVirtualRoot, builder, revno);
    collectChanges(statusHandler, filePath);
  }

  private File collectIgnored(final Set<File> retval, FilePath filePath) {
    File target = filePath.getIOFile();
    final File bzrRoot = BzrUtil.findBzrRoot(target);

    if (bzrRoot == null) {
      return null;
    }

    final String relpath = target.equals(bzrRoot) ? null : BzrUtil.relativePath(bzrRoot,target);

    final ShellCommandService service = ShellCommandService.getInstance(m_project);
    BzrIdeaExec handler = new BzrIdeaExec(BazaarRoot.findBranchLocation(target), "xmlls");
    handler.addArguments("--ignored");
    if (relpath != null)
      handler.addArguments(relpath);

    XmlOutputHandler resultHandler = new XmlOutputHandler() {
      @Override
      public void handleItem(String id, String kind, String path, String statusKind) {
        File ioFile = new File(bzrRoot, path);
        retval.add(ioFile);
      }
    };

    service.execute(handler, BzrXmlResult.createBzrXmlResult(resultHandler));
    return bzrRoot;
  }

  private File collectChanges(MyStatusHandler statusHandler, FilePath filePath) {
    File target = filePath.getIOFile();
    final File bzrRoot = BzrUtil.findBzrRoot(target);
    if (bzrRoot == null) {
      return null;
    }

    final String relpath = target.equals(bzrRoot) ? null : BzrUtil.relativePath(bzrRoot,target);

//    VirtualFile repo = VcsUtil.getVcsRootFor(m_project, filePath);
//
//    if (repo == null) {
//      return null;
//    }

    BzrIdeaExec handler = new BzrIdeaExec(BazaarRoot.findBranchLocation(target), "xmlstatus");
    if (relpath != null)
      handler.addArguments(relpath);

    final ShellCommandService service = ShellCommandService.getInstance(m_project);
    service.execute(handler, BzrXmlResult.createBzrXmlResult(statusHandler));
    return bzrRoot;
  }

  private class MyStatusHandler extends XmlOutputHandler {

    private ChangelistBuilder m_builder;
    private BzrRevisionNumber m_bzrRev;
    private VirtualFile m_vcsRoot;

    public MyStatusHandler(VirtualFile vcsRoot, ChangelistBuilder builder, BzrRevisionNumber bzrRev) {
      m_vcsRoot = vcsRoot;
      m_builder = builder;
      m_bzrRev = bzrRev;
    }

    @Override
    public void handleAdded(String kind, String path) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      Change change = new Change(null, CurrentContentRevision.create(fpath), FileStatus.ADDED);
      CHANGES.debug(String.format("%10s \"%s\"", "added", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    @Override
    public void handleModified(String kind, String path) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      BzrContentRevision bcr = BzrContentRevision.createBzrContentRevision(m_project, m_vcsRoot, fpath, m_bzrRev);
      Change change = new Change(bcr, CurrentContentRevision.create(fpath), FileStatus.MODIFIED);
      CHANGES.debug(String.format("%10s \"%s\"", "modified", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    @Override
    public void handleRemoved(String kind, String path) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      BzrContentRevision bcr = BzrContentRevision.createBzrContentRevision(m_project, m_vcsRoot, fpath, m_bzrRev);
      Change change = new Change(bcr, null, FileStatus.DELETED);
      CHANGES.debug(String.format("%10s \"%s\"", "removed", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    @Override
    public void handleRenamed(String kind, String path, String oldPath) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      FilePath oldfpath = VcsUtil.getFilePath(new File(getWorkDir(),oldPath));
      BzrContentRevision bcr = BzrContentRevision.createBzrContentRevision(m_project, m_vcsRoot, oldfpath, m_bzrRev);
      Change change = new Change(bcr, CurrentContentRevision.create(fpath), FileStatus.MODIFIED);
      CHANGES.debug(String.format("%10s \"%s\" => \"%s\"", "renamed", oldfpath, fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    @Override
    public void handleUnknown(String kind, String path) {
      File ioFile = new File(getWorkDir(), path);
      VirtualFile vFile = VcsUtil.getVirtualFileWithRefresh(ioFile);
      if (vFile == null) {
        UNKNOWN.debug(String.format("%10s skipped \"%s\"", "unknown", ioFile));
        return;
      }
      processRecursive(m_builder,vFile);
    }

    private void processRecursive(ChangelistBuilder builder, VirtualFile vFile) {
      UNKNOWN.debug(String.format("%10s \"%s\"", "unknown", vFile));
      builder.processUnversionedFile(vFile);
      if (vFile.isDirectory()) {
        for (VirtualFile child : vFile.getChildren()) {
          processRecursive(builder, child);
        }
      }
    }

    @Override
    public void handleConflicts(String path, String type) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      Change change = new Change(null, CurrentContentRevision.create(fpath), FileStatus.MERGED_WITH_CONFLICTS);
      CHANGES.debug(String.format("%10s \"%s\"", "conflict", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    @Override
    public void handleKindChanged(String kind, String path, String oldKind) {
      CHANGES.debug(String.format("%10s \"%s\"", "kind_change", path));
      super.handleKindChanged(kind, path, oldKind);
    }
  }
}
