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
package bazaar4idea.provider.annotate;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;

public class BzrAnnotationLine {

  private EnumMap<BzrAnnotation.FIELD, Object> fields =
      new EnumMap<BzrAnnotation.FIELD, Object>(BzrAnnotation.FIELD.class);

  private ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
    @Override
    public SimpleDateFormat get() {
      return new SimpleDateFormat("yyyyMMdd");
    }
  };

  public BzrAnnotationLine(String user, VcsRevisionNumber revision, String date, Integer line, String content) {
    fields.put(BzrAnnotation.FIELD.USER, user);

    fields.put(BzrAnnotation.FIELD.REVISION, revision);
    fields.put(BzrAnnotation.FIELD.DATE, date);
    fields.put(BzrAnnotation.FIELD.LINE, line);
    fields.put(BzrAnnotation.FIELD.CONTENT, content);
  }

  public VcsRevisionNumber getVcsRevisionNumber() {
    return (VcsRevisionNumber)get(BzrAnnotation.FIELD.REVISION);
  }

  public Object get(BzrAnnotation.FIELD field) {
    return fields.get(field);
  }

  public Date getDate() {
    String sDate = (String)get(BzrAnnotation.FIELD.DATE);
    if (sDate != null) {
      try {
        return dateFormat.get().parse(sDate);
      } catch (ParseException e) {
        return null;
      }
    }
    else {
      return null;
    }
  }
}
