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

package org.emergent.bzr4j.intellij;

import java.util.EventListener;

/**
 * The listener interface that allows tracking actual changes in
 * the vcs root configuration.
 */
public interface BzrRootsListener extends EventListener {
  /**
   * The method is invoked when set of actual roots changes.
   * This could happen when the list of configured roots changes or when
   * ".bzr" directories added/removed for the configured roots.
   */
  void gitRootsChanged();
}
