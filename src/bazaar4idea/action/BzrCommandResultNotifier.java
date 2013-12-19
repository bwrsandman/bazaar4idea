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
package bazaar4idea.action;

import com.intellij.openapi.project.Project;
import com.intellij.vcsUtil.VcsImplUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrStandardResult;

import java.util.List;

final class BzrCommandResultNotifier {

  private final Project project;

  BzrCommandResultNotifier(Project project) {
    this.project = project;
  }

  public void process(BzrStandardResult result) {
    List<String> out = result.getStdOutAsLines();
    List<String> err = result.getStdErrAsLines();
    if (!out.isEmpty()) {
      VcsUtil.showStatusMessage(project, out.get(out.size() - 1));
    }
    if (!err.isEmpty()) {
      VcsImplUtil.showErrorMessage(
          project, "<html>" + StringUtils.join(err, "<br>") + "</html>", "Error"
      );
    }
  }

}
