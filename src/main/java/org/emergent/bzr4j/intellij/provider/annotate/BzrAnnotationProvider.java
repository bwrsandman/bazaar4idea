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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.command.BzrAnnotateCommand;
import org.emergent.bzr4j.intellij.command.BzrLogCommand;

public class BzrAnnotationProvider implements AnnotationProvider {

  private final Project project;

  public BzrAnnotationProvider(Project project) {
    this.project = project;
  }

  public FileAnnotation annotate(VirtualFile file) throws VcsException {
    return annotate(file, null);
  }

  public FileAnnotation annotate(VirtualFile file, VcsFileRevision revision) throws VcsException {
    VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, file);
    if (vcsRoot == null) {
      throw new VcsException("vcs root is null");
    }
    BzrFile hgFile = new BzrFile(vcsRoot, VfsUtil.virtualToIoFile(file));
    BzrAnnotateCommand hgAnnotateCommand = new BzrAnnotateCommand(project);
    BzrLogCommand hgLogCommand = new BzrLogCommand(project);
    return new BzrAnnotation(
        project,
        hgFile,
        hgAnnotateCommand.execute(hgFile),
        hgLogCommand.execute(hgFile),
        null
    );
  }

  public boolean isAnnotationValid(VcsFileRevision rev) {
    return true;
  }

}
