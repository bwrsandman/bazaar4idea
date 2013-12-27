/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bazaar4idea.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.actions.ScheduleForAdditionAction;
import org.jetbrains.annotations.NotNull;

public class BzrAction extends ScheduleForAdditionAction {

  @Override
  protected boolean isStatusForAddition(FileStatus status) {
    return status == FileStatus.UNKNOWN || status == FileStatus.MODIFIED || status == FileStatus.MERGED_WITH_CONFLICTS;
  }

  /**
   * Checks if this action should be enabled.
   * Called in {@link #update(com.intellij.openapi.actionSystem.AnActionEvent)}, so don't execute long tasks here.
   * @return true if the action is enabled.
   */
  protected boolean isEnabled(@NotNull AnActionEvent event) {
    return true;
  }

}
