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

package org.emergent.bzr4j.intellij.command;

import com.intellij.openapi.util.text.LineTokenizer;
import org.apache.commons.io.LineIterator;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Patrick Woodworth
 */
public class ShellCommandServiceTest {

  @Test
  public void testLineTokenizer() {

    String[] strings = new String[] {
        "",
        "\r",
        "\n",
        "\r\n",
        "foo",
        "foo\r\nbar",
        "foo\r\nbar\r\n",
        "\r\nfoo\r\nbar\r\n",
        "foo\rbar",
        "foo\rbar\r",
        "\rfoo\rbar\r",
        "foo\nbar",
        "foo\nbar\n",
        "\nfoo\nbar\n",
        "\rfoo\nbar\r",
        "\nfoo\rbar\n",
    };

    int ii = 0;
    for (String str : strings) {
      ii++;
      List<String> answer1 = tokenize1(str);
      List<String> answer2 = tokenize2(str);
      assertEquals(answer2, answer1, "mismatch: " + ii);
    }
  }

  private List<String> tokenize1(String in) {
    return Arrays.asList(LineTokenizer.tokenize(in, false));
  }

  private List<String> tokenize2(String in) {
    ArrayList<String> retval = new ArrayList<String>();
    LineIterator iter = new LineIterator(new StringReader(in));
    while (iter.hasNext()) {
      retval.add(iter.nextLine());
    }
    return retval;
  }
}
