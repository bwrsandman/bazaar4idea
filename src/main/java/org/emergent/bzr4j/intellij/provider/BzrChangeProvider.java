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
import org.emergent.bzr4j.core.cli.BzrExecException;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.xmloutput.XmlOutputHandler;
import org.emergent.bzr4j.intellij.BzrContentRevision;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.BzrUtil;
import org.emergent.bzr4j.intellij.command.BzrIdeaExec;
import org.emergent.bzr4j.intellij.command.BzrMiscCommand;
import org.emergent.bzr4j.intellij.command.ShellCommandService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    try {
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
    } catch (BzrExecException e) {
      LOG.debug(e);
      throw new VcsException(e);
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
      Map<File, BzrRevisionNumber> processedRoots) throws BzrExecException {

    if (filePath.isNonLocal()) {
      CHANGES.debug("no processing (nonlocal path): " + String.valueOf(filePath));
      return;
    }

    File target = filePath.getIOFile();
    BazaarRoot bzrRoot = BazaarRoot.findBranchLocation(target);

    if (bzrRoot == null) {
      CHANGES.debug("no processing (no io root): " + String.valueOf(filePath));
      return;
    }

    final File ioRoot = bzrRoot.getFile();

    final String relpath = target.equals(ioRoot) ? null : BzrUtil.relativePath(ioRoot,target);

    CHANGES.debug("is processing: " + String.valueOf(filePath));

    BzrRevisionNumber revno = processedRoots.get(ioRoot);
    if (revno == null) {
      revno = BzrMiscCommand.revno(m_project,vcsVirtualRoot);
      processedRoots.put(ioRoot,revno);
    }

    final ShellCommandService service = ShellCommandService.getInstance(m_project);

    MyIgnoredHandler ignoredHandler = new MyIgnoredHandler(builder, ioRoot);

    BzrIdeaExec ignoredExec = new BzrIdeaExec(bzrRoot, "xmlls");
    ignoredExec.addArguments("--ignored");
    if (relpath != null)
      ignoredExec.addArguments(relpath);
    service.executeUnsafe(ignoredExec, BzrXmlResult.createBzrXmlResult(ignoredHandler));
    ignoredHandler.processResults();

    MyStatusHandler statusHandler = new MyStatusHandler(vcsVirtualRoot, builder, ioRoot, revno);

    BzrIdeaExec statusExec = new BzrIdeaExec(bzrRoot, "xmlstatus");
    statusExec.setStderrValidationEnabled(false);
    if (relpath != null)
      statusExec.addArguments(relpath);
    service.executeUnsafe(statusExec, BzrXmlResult.createBzrXmlResult(statusHandler));

    statusHandler.processResults();
  }

  private class MyIgnoredHandler extends XmlOutputHandler {

    private ChangelistBuilder m_builder;
    private File m_bzrRoot;
    private final Collection<String> m_ignoredList = new LinkedList<String>();

    public MyIgnoredHandler(ChangelistBuilder builder, File bzrRoot) {
      m_builder = builder;
      m_bzrRoot = bzrRoot;
    }

    public void processResults() {
      for (String path : m_ignoredList) {
        processIgnore(path);
      }
    }

    @Override
    public void handleItem(String id, String kind, String path, String statusKind) {
      m_ignoredList.add(path);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      LOG.warn(e, "sax error");
    }

    private void processIgnore(String path) {
      File ignored = new File(m_bzrRoot, path);
      IGNORED.debug(String.format("%10s \"%s\"", "ignored", ignored));
      m_builder.processIgnoredFile(VcsUtil.getVirtualFile(ignored));
    }
  }

  private class MyStatusHandler extends XmlOutputHandler {

    private ChangelistBuilder m_builder;
    private BzrRevisionNumber m_bzrRev;
    private VirtualFile m_vcsRoot;
    private File m_bzrRoot;
    private final Collection<GenericChange> m_changes = new LinkedList<GenericChange>();

    public MyStatusHandler(VirtualFile vcsRoot, ChangelistBuilder builder, File bzrRoot, BzrRevisionNumber bzrRev) {
      m_vcsRoot = vcsRoot;
      m_builder = builder;
      m_bzrRoot = bzrRoot;
      m_bzrRev = bzrRev;
    }

    public void processResults() {
      for (GenericChange change : m_changes) {
        processGenericChange(change.m_changeType, change.m_kind, change.m_path, change.m_attributes);
      }
    }
    
    @Override
    public File getWorkDir() {
      return m_bzrRoot;
    }
        
    @Override
    public void handleGenericChange(String changeType, String kind, String path, Attributes attributes) {
      m_changes.add(new GenericChange(changeType, kind, path, attributes));
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      LOG.warn(e, "sax error");
    }

    private void processGenericChange(String changeType, String kind, String path, Attributes attributes) {
      if ("added".equals(changeType)) {
        processAdded(kind, path);
      } else if ("modified".equals(changeType)) {
        processModified(kind, path);
      } else if ("removed".equals(changeType)) {
        processRemoved(kind, path);
      } else if ("renamed".equals(changeType)) {
        processRenamed(kind, path, attributes.getValue("oldpath"));
      } else if ("unknown".equals(changeType)) {
        processUnknown(kind, path);
      } else if ("conflicts".equals(changeType)) {
        processConflicts(path, attributes.getValue("type"));
      } else if ("kind_changed".equals(changeType)) {
        processKindChanged(kind, path, attributes.getValue("oldkind"));
      }
    }
    
    private void processAdded(String kind, String path) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      Change change = new Change(null, CurrentContentRevision.create(fpath), FileStatus.ADDED);
      CHANGES.debug(String.format("%10s \"%s\"", "added", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    private void processModified(String kind, String path) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      BzrContentRevision bcr = BzrContentRevision.createBzrContentRevision(m_project, m_vcsRoot, fpath, m_bzrRev);
      Change change = new Change(bcr, CurrentContentRevision.create(fpath), FileStatus.MODIFIED);
      CHANGES.debug(String.format("%10s \"%s\"", "modified", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    private void processRemoved(String kind, String path) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      BzrContentRevision bcr = BzrContentRevision.createBzrContentRevision(m_project, m_vcsRoot, fpath, m_bzrRev);
      Change change = new Change(bcr, null, FileStatus.DELETED);
      CHANGES.debug(String.format("%10s \"%s\"", "removed", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    private void processRenamed(String kind, String path, String oldPath) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      FilePath oldfpath = VcsUtil.getFilePath(new File(getWorkDir(),oldPath));
      BzrContentRevision bcr = BzrContentRevision.createBzrContentRevision(m_project, m_vcsRoot, oldfpath, m_bzrRev);
      Change change = new Change(bcr, CurrentContentRevision.create(fpath), FileStatus.MODIFIED);
      CHANGES.debug(String.format("%10s \"%s\" => \"%s\"", "renamed", oldfpath, fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    private void processUnknown(String kind, String path) {
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

    private void processConflicts(String path, String type) {
      FilePath fpath = VcsUtil.getFilePath(new File(getWorkDir(),path));
      Change change = new Change(null, CurrentContentRevision.create(fpath), FileStatus.MERGED_WITH_CONFLICTS);
      CHANGES.debug(String.format("%10s \"%s\"", "conflict", fpath));
      m_builder.processChange(change, m_vcsKey);
    }

    private void processKindChanged(String kind, String path, String oldKind) {
      CHANGES.debug(String.format("%10s \"%s\"", "kind_change", path));
      super.handleKindChanged(kind, path, oldKind);
    }

    private class GenericChange {
      
      public final String m_changeType;
      public final String m_kind;
      public final String m_path;
      public final Attributes m_attributes;

      public GenericChange(String changeType, String kind, String path, Attributes attributes) {
        m_changeType = changeType;
        m_kind = kind;
        m_path = path;
        m_attributes = attributes;
      }
    }    
  }
}
