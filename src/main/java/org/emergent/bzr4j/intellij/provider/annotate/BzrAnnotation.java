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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.AnnotationListener;
import com.intellij.openapi.vcs.annotate.AnnotationSourceSwitcher;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.BzrFileRevision;

import java.util.LinkedList;
import java.util.List;

public class BzrAnnotation implements FileAnnotation {

  private static final Logger LOG = Logger.getInstance(BzrAnnotation.class.getName());

  enum FIELD {

    USER, REVISION, DATE, LINE, CONTENT
  }

  private final HgLineAnnotationAspect dateAnnotationAspect = new HgLineAnnotationAspect(FIELD.DATE);

  private final HgLineAnnotationAspect userAnnotationAspect = new HgLineAnnotationAspect(FIELD.USER);

  private final HgLineAnnotationAspect revisionAnnotationAspect = new HgLineAnnotationAspect(FIELD.REVISION);

  private final List<BzrAnnotationLine> lines;
  private final List<VcsFileRevision> vcsFileRevisions;
  private final BzrFile hgFile;

  public BzrAnnotation(BzrFile hgFile, List<BzrAnnotationLine> lines, List<VcsFileRevision> vcsFileRevisions) {
    this.lines = lines;
    this.vcsFileRevisions = vcsFileRevisions;
    this.hgFile = hgFile;
  }

  public AnnotationSourceSwitcher getAnnotationSourceSwitcher() {
    return null;
  }

  public VcsRevisionNumber originalRevision(int lineNumber) {
    return getLineRevisionNumber(lineNumber);
  }

  public boolean revisionsNotEmpty() {
    return true;
  }

  public void addListener(AnnotationListener listener) {
  }

  public void removeListener(AnnotationListener listener) {
  }

  public void dispose() {
  }

  public LineAnnotationAspect[] getAspects() {
    return new LineAnnotationAspect[] {
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
      return CurrentContentRevision.create(hgFile.toFilePath()).getContent();
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

  public List<VcsFileRevision> getRevisions() {
    List<VcsFileRevision> result = new LinkedList<VcsFileRevision>();
    result.addAll(vcsFileRevisions);
    return result;
  }

  class HgLineAnnotationAspect implements LineAnnotationAspect {

    private final FIELD aspectType;

    public HgLineAnnotationAspect(FIELD aspectType) {
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

    public String getTooltipText(int lineNumber) {
      return null;
    }

    public String getId() {
      return null;
    }

    public boolean isShowByDefault() {
      return false;
    }
  }

}
