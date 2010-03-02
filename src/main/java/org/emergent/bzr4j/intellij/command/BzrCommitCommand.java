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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.emergent.bzr4j.intellij.BzrFile;
import org.emergent.bzr4j.intellij.BzrVcs;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BzrCommitCommand {

  private static final Logger LOG = Logger.getInstance(BzrCommitCommand.class.getName());

  private static final String TEMP_FILE_NAME = ".bzr4intellij-commit.tmp";

  private final Project project;
  private final VirtualFile repo;
  private final String message;

  private List<BzrFile> files = Collections.emptyList();

  public BzrCommitCommand(Project project, @NotNull VirtualFile repo, String message) {
    this.project = project;
    this.repo = repo;
    this.message = message;
  }

  public void setFiles(@NotNull List<BzrFile> files) {
    this.files = files;
  }

  public void execute() throws BzrCommandException {
    if (StringUtils.isBlank(message)) {
      Object[] params = new Object[] { };
      throw new BzrCommandException(BzrVcsMessages.message("bzr4intellij.commit.error.messageEmpty", params));
    }
    try {
      List<String> parameters = new LinkedList<String>();
      parameters.add("-F");
      parameters.add(saveCommitMessage().getAbsolutePath());
      for (BzrFile hgFile : files) {
        parameters.add(hgFile.getRelativePath());
      }
      BzrIdeaExec shellCommand = ShellCommandService.getInstance(project).createCommand(repo, "commit");
      shellCommand.addArguments(parameters);
      shellCommand.setStderrValidationEnabled(false);
      ShellCommandService.getInstance(project).execute(shellCommand);
      project.getMessageBus().syncPublisher(BzrVcs.OUTGOING_TOPIC).update(project);
    } catch (IOException e) {
      LOG.error(e);
    }
  }

  private File saveCommitMessage() throws IOException {
    File systemDir = new File(PathManager.getSystemPath());
    File tempFile = new File(systemDir, TEMP_FILE_NAME);
    Writer output = new BufferedWriter(new FileWriter(tempFile, false));
    try {
      output.write(message);
      output.flush();
    } finally {
      output.close();
    }
    tempFile.deleteOnExit();
    return tempFile;
  }

}
