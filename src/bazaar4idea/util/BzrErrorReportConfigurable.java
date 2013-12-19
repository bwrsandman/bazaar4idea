/*
 * Copyright (c) 2010 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package bazaar4idea.util;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import org.apache.commons.codec.binary.Base64;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class BzrErrorReportConfigurable implements JDOMExternalizable, ApplicationComponent {
  public String AUTH_USERNAME = "";
  public String AUTH_PASSWORD = "";
  public boolean SAVE_PASSWORD = false;

  public String EMAIL_ADDRESS = "";
  public String SMTP_SERVER = "";

  public static BzrErrorReportConfigurable getInstance() {
    return ServiceManager.getService(BzrErrorReportConfigurable.class);
  }

  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
    if (!SAVE_PASSWORD)
      AUTH_PASSWORD = "";
  }

  public void writeExternal(Element element) throws WriteExternalException {
    String itnPassword = AUTH_PASSWORD;
    if (!SAVE_PASSWORD)
      AUTH_PASSWORD = "";
    DefaultJDOMExternalizer.writeExternal(this, element);

    AUTH_PASSWORD = itnPassword;
  }

  @NotNull
  public String getComponentName() {
    return "BzrErrorReportConfigurable";
  }

  public void initComponent() { }

  public void disposeComponent() {
  }

  public String getPlainItnPassword () {
    if (AUTH_PASSWORD == null || "".equals(AUTH_PASSWORD.trim()))
      return "";
    return new String(new Base64().decode(AUTH_PASSWORD.getBytes()));
  }

  public void setPlainItnPassword (String password) {
    if (password == null || "".equals(password.trim())) {
      AUTH_PASSWORD = "";
    } else {
      AUTH_PASSWORD = new String(new Base64().encode(password.getBytes()));
    }
  }
}
