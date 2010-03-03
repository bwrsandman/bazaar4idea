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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.util.Consumer;
import org.emergent.bzr4j.core.cli.BzrXmlResult;
import org.emergent.bzr4j.core.xmloutput.XmlOutputHandler;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.BzrFileRevision;
import org.emergent.bzr4j.intellij.BzrRevisionNumber;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class BzrLogCommand extends BzrAbstractCommand {

  private static final Logger LOG = Logger.getInstance(BzrLogCommand.class.getName());

  private static final int REVISION_INDEX = 0;

  public BzrLogCommand(Project project) {
    super(project);
  }

  public final List<VcsFileRevision> execute(final BzrFile bzrFile, int limit) {
    final List<VcsFileRevision> revisions = new LinkedList<VcsFileRevision>();

    execute(bzrFile, limit, new Consumer<VcsFileRevision>() {
      public void consume(VcsFileRevision revision) {
        revisions.add(revision);
      }
    });

    return revisions;
  }

  public final void execute(final BzrFile bzrFile, int limit, final Consumer<VcsFileRevision> consumer) {
    if (limit <= REVISION_INDEX || bzrFile == null || bzrFile.getRepo() == null) {
      return;
    }

    BzrIdeaExec handler = BzrIdeaExec.createBzrIdeaExec(bzrFile.getRepo(), "xmllog");

    ShellCommandService bzrCommandService = ShellCommandService.getInstance(project);

    List<String> arguments = new LinkedList<String>();
    arguments.add(bzrFile.getRelativePath());

    handler.addArguments(arguments);

    XmlOutputHandler resultHandler = new XmlOutputHandler() {
      @Override
      public void handleLog(String revno, String committer, String branchNick, Date timestamp, String message) {
        BzrRevisionNumber bzrRev = BzrRevisionNumber.getLocalInstance(revno);
        consumer.consume(new BzrFileRevision(project, bzrFile, bzrRev, branchNick, timestamp, committer, message));
      }
    };

    bzrCommandService.execute(handler, BzrXmlResult.createBzrXmlResult(resultHandler));
  }

}
