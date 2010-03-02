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
package org.emergent.bzr4j.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;

import javax.swing.*;

public class BzrChangesetStatus extends JLabel {

  public BzrChangesetStatus(Icon icon) {
    super(icon, SwingConstants.TRAILING);
    setVisible(false);
  }

  public void setChanges(final int count, final ChangesetWriter formatter) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (count == 0) {
          setVisible(false);
          return;
        }
        setText(String.valueOf(count));
        setToolTipText(formatter.asString());
        setVisible(true);
      }
    });
  }

  public interface ChangesetWriter {

    String asString();
  }

}
