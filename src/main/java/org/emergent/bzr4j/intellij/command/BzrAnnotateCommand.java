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
package org.emergent.bzr4j.intellij.command;

import com.intellij.openapi.project.Project;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.xmloutput.XmlOutputHandler;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.BzrGlobalSettings;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;
import org.emergent.bzr4j.intellij.provider.annotate.BzrAnnotationLine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BzrAnnotateCommand extends BzrAbstractCommand {

  public BzrAnnotateCommand(Project project) {
    super(project);
  }

  public List<BzrAnnotationLine> execute(@NotNull final BzrFile hgFile) {
    BzrIdeaExec handler = BzrIdeaExec.createBzrIdeaExec(hgFile.getRepo(), "xmlannotate");

    ShellCommandService hgCommandService = ShellCommandService.getInstance(project);

    List<String> arguments = new LinkedList<String>();
    arguments.add(hgFile.getRelativePath());

    handler.addArguments(arguments);

    final List<BzrAnnotationLine> annotations = new ArrayList<BzrAnnotationLine>();
    XmlOutputHandler resultHandler = new XmlOutputHandler() {
      @Override
      public void handleAnnotationEntry(String content, String revno, String author, String date) {
        BzrRevisionNumber revision = BzrRevisionNumber.getLocalInstance(revno);
        String user = author;
        int atIdx = user.indexOf('@');
        if (atIdx > 0 && BzrGlobalSettings.getInstance().isAnnotationTrimmingEnabled()) {
          user = user.substring(0, atIdx);
        }
        // convert the strftime'%Y%m%d' - '%Y-%m-%d' to match the command line tool
        String expandedDate = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
        BzrAnnotationLine annotationLine = new BzrAnnotationLine(user, revision, expandedDate,
          annotations.size(), content);
        annotations.add(annotationLine);
      }
    };

    hgCommandService.execute(handler, BzrXmlResult.createBzrXmlResult(resultHandler));
    return annotations;
  }

}
