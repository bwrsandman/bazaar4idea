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
package bazaar4idea.data;

public enum BzrResolveStatusEnum {

  UNRESOLVED('U'),
  RESOLVED('R');

  private final char status;

  BzrResolveStatusEnum(char status) {
    this.status = status;
  }

  public char getStatus() {
    return status;
  }

  public static BzrResolveStatusEnum valueOf(char status) {
    if (status == UNRESOLVED.status) {
      return UNRESOLVED;
    }
    if (status == RESOLVED.status) {
      return RESOLVED;
    }
    return null;
  }
}
