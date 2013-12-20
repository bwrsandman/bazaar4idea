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
package bazaar4idea.checkin;

import bazaar4idea.BzrVcs;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Prohibits committing with an empty messages, warns if committing into detached HEAD, checks if user name and correct
 * CRLF attributes are set.
 * @author Kirill Likhodedov
*/
public class BzrCheckinHandlerFactory extends VcsCheckinHandlerFactory {

  private static final Logger LOG = Logger.getInstance(BzrCheckinHandlerFactory.class);

  public BzrCheckinHandlerFactory() {
    super(BzrVcs.getKey());
  }

  @NotNull
  @Override
  protected CheckinHandler createVcsHandler(final CheckinProjectPanel panel) {
    return new MyCheckinHandler(panel);
  }

  private class MyCheckinHandler extends CheckinHandler {
    @NotNull private final CheckinProjectPanel myPanel;
    @NotNull private final Project myProject;


    public MyCheckinHandler(@NotNull CheckinProjectPanel panel) {
      myPanel = panel;
      myProject = myPanel.getProject();
    }

  }

}