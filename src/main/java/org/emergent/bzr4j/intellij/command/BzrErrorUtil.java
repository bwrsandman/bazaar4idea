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

import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.core.cli.BzrAbstractResult;

import java.util.List;

final class BzrErrorUtil {

  private BzrErrorUtil() {
  }

  static boolean isAbort(BzrAbstractResult result) {
    String line = getLastErrorLine(result);
    return StringUtils.isNotBlank(line) && StringUtils.contains(line, "abort:");
  }

  static boolean isAuthorizationRequiredAbort(BzrAbstractResult result) {
    String line = getLastErrorLine(result);
    return StringUtils.isNotBlank(line) && (
        StringUtils.contains(line, "authorization required")
            || StringUtils.contains(line, "authorization failed")
    );
  }

  private static String getLastErrorLine(BzrAbstractResult result) {
    List<String> errorLines = result.getStdErrAsLines();
    if (errorLines.isEmpty()) {
      return null;
    }
    return errorLines.get(errorLines.size() - 1);
  }


}
