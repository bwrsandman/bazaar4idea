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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.Map;
import java.util.TreeMap;

@State(
    name = "bzr4intellij.settings",
    storages = @Storage(id = "bzr4intellij.settings", file = "$OPTIONS$/bzr4intellij.xml")
)
public class BzrGlobalSettings implements PersistentStateComponent<BzrGlobalSettings> {

  public static final String DEFAULT_EXECUTABLE = "bzr";
  private static final int FIVE_MINUTES = 300;

  private final TreeMap<String, String> m_environmentVariables = new TreeMap<String, String>();

  private String m_bzrExecutable = DEFAULT_EXECUTABLE;
  private boolean m_annotationTrimmingEnabled;
  private boolean m_modalErrorPopupEnabled;
  private boolean m_granularExecLockingEnabled;

  public static BzrGlobalSettings getInstance() {
    return ServiceManager.getService(BzrGlobalSettings.class);
  }

  public BzrGlobalSettings getState() {
    return this;
  }

  public void loadState(BzrGlobalSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public String getBzrExecutable() {
    return m_bzrExecutable;
  }

  public void setBzrExecutable(String bzrExecutable) {
    m_bzrExecutable = bzrExecutable;
  }

  public boolean isAnnotationTrimmingEnabled() {
    return m_annotationTrimmingEnabled;
  }

  public void setAnnotationTrimmingEnabled(boolean annotationTrimmingEnabled) {
    m_annotationTrimmingEnabled = annotationTrimmingEnabled;
  }

  public int getIncomingCheckIntervalSeconds() {
    return FIVE_MINUTES;
  }

  public boolean isGranularExecLockingEnabled() {
    return m_granularExecLockingEnabled;
  }

  public void setGranularExecLockingEnabled(boolean granularExecLockingEnabled) {
    m_granularExecLockingEnabled = granularExecLockingEnabled;
  }

  public boolean isModalErrorPopupEnabled() {
    return m_modalErrorPopupEnabled;
  }

  public void setModalErrorPopupEnabled(boolean val) {
    m_modalErrorPopupEnabled = val;
  }

  public Map<String, String> getEnvironmentVariables() {
    return new TreeMap<String,String>(m_environmentVariables);
  }

  public void setEnvironmentVariables(Map<String, String> val) {
    m_environmentVariables.clear();
    m_environmentVariables.putAll(val);
  }

  public String getBzrEnvVar(String key) {
    return m_environmentVariables.get(key);
  }

  public String getBzrEnvVarSafe(String key) {
    String retval = m_environmentVariables.get(key);
    return retval != null ? retval : "";
  }

  public void setBzrEnvVarSafe(String key, String val) {
    if (val != null) {
      m_environmentVariables.put(key, val);
    } else {
      m_environmentVariables.remove(key);
    }
  }
}
