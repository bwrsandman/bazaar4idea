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
package bazaar4idea;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(
    name = "bzr4intellij.settings",
    storages = @Storage(id = "bzr4intellij.settings", file = "$PROJECT_FILE$")
)
public class BzrProjectSettings implements PersistentStateComponent<BzrProjectSettings> {

  private boolean checkIncoming;
  private boolean checkOutgoing;
  private boolean m_scanTargetOptimizationEnabled;
  private boolean m_rootCheckingEnabled = true;

  public static BzrProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, BzrProjectSettings.class);
  }

  public BzrProjectSettings getState() {
    return this;
  }

  public void loadState(BzrProjectSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isCheckIncoming() {
    return checkIncoming;
  }

  public void setCheckIncoming(boolean checkIncoming) {
    this.checkIncoming = checkIncoming;
  }

  public boolean isCheckOutgoing() {
    return checkOutgoing;
  }

  public void setCheckOutgoing(boolean checkOutgoing) {
    this.checkOutgoing = checkOutgoing;
  }

  public boolean isScanTargetOptimizationEnabled() {
    return m_scanTargetOptimizationEnabled;
  }

  public void setScanTargetOptimizationEnabled(boolean scanTargetOptimizationEnabled) {
    m_scanTargetOptimizationEnabled = scanTargetOptimizationEnabled;
  }

  public boolean isRootCheckingEnabled() {
    return m_rootCheckingEnabled;
  }

  public void setRootCheckingEnabled(boolean val) {
    m_rootCheckingEnabled = val;
  }
}
