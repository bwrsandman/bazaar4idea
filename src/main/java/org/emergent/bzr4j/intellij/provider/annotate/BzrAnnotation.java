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
package org.emergent.bzr4j.intellij.provider.annotate;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.*;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.intellij.BzrFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class BzrAnnotation extends FileAnnotation {

  private static final Logger LOG = Logger.getInstance(BzrAnnotation.class.getName());

  enum FIELD {

    USER, REVISION, DATE, LINE, CONTENT
  }

  private final HgLineAnnotationAspect dateAnnotationAspect = new HgLineAnnotationAspect(FIELD.DATE);

  private final HgLineAnnotationAspect userAnnotationAspect = new HgLineAnnotationAspect(FIELD.USER);

  private final HgLineAnnotationAspect revisionAnnotationAspect = new HgLineAnnotationAspect(FIELD.REVISION);

  private final List<BzrAnnotationLine> lines;
  private final List<VcsFileRevision> vcsFileRevisions;
  private final BzrFile bzrFile;
  private final VcsRevisionNumber currentRevision;


  public BzrAnnotation(@NotNull Project project, BzrFile bzrFile, List<BzrAnnotationLine> lines, List<VcsFileRevision> vcsFileRevisions,
                       VcsRevisionNumber revision) {
    super(project);
    this.lines = lines;
    this.vcsFileRevisions = vcsFileRevisions;
    this.bzrFile = bzrFile;
    this.currentRevision = revision;
  }

  public AnnotationSourceSwitcher getAnnotationSourceSwitcher() {
    return null;
  }

  public VcsRevisionNumber originalRevision(int lineNumber) {
    return getLineRevisionNumber(lineNumber);
  }

  @Nullable
  @Override
  public VcsRevisionNumber getCurrentRevision() {
    return currentRevision;
  }

  public boolean revisionsNotEmpty() {
    return true;
  }

  public void dispose() {
  }

  public LineAnnotationAspect[] getAspects() {
    return new LineAnnotationAspect[]{
        revisionAnnotationAspect,
        dateAnnotationAspect,
        userAnnotationAspect
    };
  }

  public String getToolTip(int lineNumber) {
    return null;
  }

  public String getAnnotatedContent() {
    try {
      return CurrentContentRevision.create(bzrFile.toFilePath()).getContent();
    } catch (VcsException e) {
      LOG.error(e);
      return StringUtils.EMPTY;
    }
  }

  public VcsRevisionNumber getLineRevisionNumber(int lineNumber) {
    if (lineNumber >= lines.size() || lineNumber < 0) {
      return null;
    }
    BzrAnnotationLine annotationLine = lines.get(lineNumber);
    return annotationLine.getVcsRevisionNumber();
  }

  @Override
  public Date getLineDate(int lineNumber) {
    if (lineNumber >= lines.size() || lineNumber < 0) {
      return null;
    }
    return lines.get(lineNumber).getDate();
  }

  public List<VcsFileRevision> getRevisions() {
    List<VcsFileRevision> result = new LinkedList<VcsFileRevision>();
    result.addAll(vcsFileRevisions);
    return result;
  }

  @Override
  public int getLineCount() {
    return lines.size();
  }

  private static String id(FIELD field) {
    switch (field) {
      case USER:
        return LineAnnotationAspect.AUTHOR;
      case REVISION:
        return LineAnnotationAspect.REVISION;
      case DATE:
        return LineAnnotationAspect.DATE;
      default:
        return null;
    }
  }

  private static boolean isShowByDefault(FIELD aspectType) {
    return aspectType == FIELD.DATE || aspectType == FIELD.USER;
  }

  class HgLineAnnotationAspect extends LineAnnotationAspectAdapter {

    private final FIELD aspectType;

    public HgLineAnnotationAspect(FIELD aspectType) {
      super(id(aspectType), BzrAnnotation.isShowByDefault(aspectType));
      this.aspectType = aspectType;
    }

    public String getValue(int lineNumber) {
      if (lineNumber >= lines.size() || lineNumber < 0) {
        return StringUtils.EMPTY;
      }
      BzrAnnotationLine annotationLine = lines.get(lineNumber);
      return aspectType == FIELD.REVISION
          ? annotationLine.getVcsRevisionNumber().asString()
          : annotationLine.get(aspectType).toString();
    }

    @Override
    protected void showAffectedPaths(int lineNum) {
      // todo
    }
  }

  @Override
  public VirtualFile getFile() {
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(bzrFile.getFile());
  }
}
