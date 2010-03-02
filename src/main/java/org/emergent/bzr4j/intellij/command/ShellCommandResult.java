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

import com.intellij.openapi.util.text.LineTokenizer;
import org.emergent.bzr4j.core.cli.BzrHandlerResult;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public final class ShellCommandResult extends BzrHandlerResult {

  static final ShellCommandResult EMPTY = new ShellCommandResult();

  private List<String> outLines;
  private List<String> errLines;

  public ShellCommandResult() {
    super();
  }

  public ShellCommandResult(Charset charset) {
    super(charset);
  }

  public List<String> getOutputLines() {
    if (outLines == null) {
      outLines = Arrays.asList(LineTokenizer.tokenize(getStdOutAsString(), false));
    }
    return outLines;
  }

  public List<String> getErrorLines() {
    if (errLines == null) {
      errLines = Arrays.asList(LineTokenizer.tokenize(getStdErrAsString(), false));
    }
    return errLines;
  }
}
