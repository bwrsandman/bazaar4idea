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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.emergent.bzr4j.core.cli.BzrStandardResult;
import bazaar4idea.BzrRevisionNumber;
import bazaar4idea.data.BzrTagBranch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BzrTagBranchCommand {

  private static final Pattern BRANCH_LINE = Pattern.compile("(.+)\\s([0-9]+):([0-9a-f]+).*");
  private static final int NAME_INDEX = 1;
  private static final int REVISION_INDEX = 2;
  private static final int CHANGESET_INDEX = 3;

  private final Project project;
  private final VirtualFile repo;

  public BzrTagBranchCommand(Project project, @NotNull VirtualFile repo) {
    this.project = project;
    this.repo = repo;
  }

  @Nullable
  public String getCurrentBranch() {
//        ShellCommandResult result = ShellCommandService.getInstance(project).execute2(repo, "branch", null);
//        List<String> output = result.getOutputLines();
//        if (output == null || output.isEmpty()) {
    return null;
//        }
//        return StringUtils.trim(output.get(0));
  }

  public List<BzrTagBranch> listBranches() {
    List<BzrTagBranch> retval = new LinkedList<BzrTagBranch>();
//    BzrInfoCommand command = new BzrInfoCommand(project);
//    IBazaarInfo info = command.execute(repo);
//    if (info != null) {
//      IBazaarInfo.RelatedBranches related = info.getRelatedBranches();
//      addBranch(retval, "submit", related.getSubmitBranch(), null);
//      addBranch(retval, "parent", related.getParentBranch(), null);
//      addBranch(retval, "push", related.getPushBranch(), null);
//      addBranch(retval, "public", related.getPublicBranch(), null);
//    }
    return retval;
  }

  private void addBranch(List<BzrTagBranch> list, String name, String desc, BzrRevisionNumber rev) {
    if (desc != null) {
      list.add(new BzrTagBranch(name, desc, rev));
    }
  }

  public List<BzrTagBranch> listTags() {
//        return tokenize(ShellCommandService.getInstance(project).execute2(repo, "tags", null));
    return Collections.emptyList();
  }

  private List<BzrTagBranch> tokenize(BzrStandardResult result) {
    List<BzrTagBranch> branches = new LinkedList<BzrTagBranch>();
    for (final String line : result.getStdOutAsLines()) {
      Matcher matcher = BRANCH_LINE.matcher(line);
      if (matcher.matches()) {
        BzrRevisionNumber hgRevisionNumber = BzrRevisionNumber.getInstance(
            matcher.group(REVISION_INDEX), matcher.group(CHANGESET_INDEX)
        );
        branches.add(new BzrTagBranch(matcher.group(NAME_INDEX).trim(), line, hgRevisionNumber));
      }
    }
    return branches;
  }

}
