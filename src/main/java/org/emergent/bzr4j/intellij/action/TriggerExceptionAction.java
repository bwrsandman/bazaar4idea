package org.emergent.bzr4j.intellij.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class TriggerExceptionAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        throw new RuntimeException("I'm an artificial exception!");
    }
}
