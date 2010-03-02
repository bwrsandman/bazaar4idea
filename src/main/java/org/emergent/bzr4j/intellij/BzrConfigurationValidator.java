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
package org.emergent.bzr4j.intellij;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.emergent.bzr4j.intellij.BzrGlobalSettings;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.emergent.bzr4j.intellij.command.ShellCommandService;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.concurrent.atomic.AtomicBoolean;

class BzrConfigurationValidator {

  private static final String BZR_INVALID_CONFIG_ID = "BazaarConfig";

  private static final AtomicBoolean m_exeNotificationPosted = new AtomicBoolean(false);

  private static Notification m_exeNotification;

  private final Project m_project;

  public BzrConfigurationValidator(Project project) {
    m_project = project;
  }

  /**
   * Check executable.
   */
  public void check() {

    final BzrGlobalSettings globalSettings = BzrGlobalSettings.getInstance();

    boolean hasInvalidExe = !doActualCheck(globalSettings);

    if (!hasInvalidExe) {
      // all roots are correct
      if (m_exeNotificationPosted.compareAndSet(true, false)) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          public void run() {
            if (m_exeNotification != null) {
              if (!m_exeNotification.isExpired()) {
                m_exeNotification.expire();
              }
              m_exeNotification = null;
            }
          }
        });
      }
    } else if (m_exeNotificationPosted.compareAndSet(false, true)) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        public void run() {
          m_exeNotification = new Notification(BZR_INVALID_CONFIG_ID,
              BzrVcsMessages.message("exe.tracker.message.title"),
              BzrVcsMessages.message("exe.tracker.message"),
              NotificationType.ERROR,
              new NotificationListener() {
                public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                  if (check(globalSettings)) {
                    notification.expire();
                  }
                }
              });

          Notifications.Bus.notify(m_exeNotification, m_project);
        }
      });
    }
  }

  private static boolean check(final BzrGlobalSettings globalSettings) {
    if (doActualCheck(globalSettings)) {
      return true;
    }

    ShowSettingsUtil.getInstance().showSettingsDialog(null, BzrIdeConfigurable.class);

    return doActualCheck(globalSettings);
  }

  private static boolean doActualCheck(final BzrGlobalSettings globalSettings) {
    return ShellCommandService.isValid(globalSettings.getBzrExecutable());
  }
}
