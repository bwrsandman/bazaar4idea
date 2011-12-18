/*
 * Copyright (c) 2011 Francis Devereux
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

package org.emergent.bzr4j.intellij.provider.annotate;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Francis Devereux
 */
public class BzrAnnotationLineTest {
  @Test
  public void testGetDate() {
    BzrAnnotationLine line = new BzrAnnotationLine("user", null, "20091210", 32, "    public static void main(String args[])");
    Date d = line.getDate();
    Calendar c = Calendar.getInstance();
    c.setTime(d);
    assertEquals(2009, c.get(Calendar.YEAR));
    assertEquals(11, c.get(Calendar.MONTH));
    assertEquals(10, c.get(Calendar.DAY_OF_MONTH));
  }
}
