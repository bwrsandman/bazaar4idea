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
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import bazaar4idea.BzrFile;
import bazaar4idea.BzrFileRevision;
import bazaar4idea.BzrRevisionNumber;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

abstract class BzrRevisionsCommand {

  private static final Logger LOG = Logger.getInstance(BzrRevisionsCommand.class.getName());

  private static final String TEMPLATE =
      "{rev}|{node|short}|{date|isodate}|{author}|{branches}|{desc}\\n";

  private static final int REVISION_INDEX = 0;
  private static final int CHANGESET_INDEX = 1;
  private static final int DATE_INDEX = 2;
  private static final int AUTHOR_INDEX = 3;
  private static final int BRANCH_INDEX = 4;
  private static final int MESSAGE_INDEX = 5;
  private static final int ITEM_COUNT = 6;

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm Z", Locale.US);

  private final Project project;

  public BzrRevisionsCommand(Project project) {
    this.project = project;
  }

  protected abstract BzrStandardResult execute(
      ShellCommandService service, VirtualFile repo, String template, int limit, BzrFile hgFile
  );

  public final List<BzrFileRevision> execute(BzrFile hgFile, int limit) {
    if (limit <= REVISION_INDEX || hgFile == null || hgFile.getRepo() == null) {
      return Collections.emptyList();
    }

    ShellCommandService hgCommandService = ShellCommandService.getInstance(project);

    BzrStandardResult result = execute(
        hgCommandService, hgFile.getRepo(), TEMPLATE, limit, hgFile
    );

    List<BzrFileRevision> revisions = new LinkedList<BzrFileRevision>();
    for (String line : result.getStdOutAsLines()) {
      try {
        String[] attributes = StringUtils.splitPreserveAllTokens(line, '|');
        if (attributes.length != ITEM_COUNT) {
          LOG.warn("Wrong format. Skipping line " + line);
          continue;
        }
        BzrRevisionNumber vcsRevisionNumber = BzrRevisionNumber.getInstance(
            attributes[REVISION_INDEX],
            attributes[CHANGESET_INDEX]
        );
        Date revisionDate = DATE_FORMAT.parse(attributes[DATE_INDEX]);
        String author = attributes[AUTHOR_INDEX];
        String branchName = attributes[BRANCH_INDEX];
        String commitMessage = attributes[MESSAGE_INDEX];
        revisions.add(new BzrFileRevision(project, hgFile, vcsRevisionNumber,
            branchName, revisionDate, author, commitMessage));
      } catch (NumberFormatException e) {
        LOG.warn("Error parsing rev in line " + line);
      } catch (ParseException e) {
        LOG.warn("Error parsing date in line " + line);
      }
    }
    return revisions;
  }


}
