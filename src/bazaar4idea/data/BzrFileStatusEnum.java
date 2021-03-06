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

public enum BzrFileStatusEnum {

  VERSIONED('+'),
  UNVERSIONED('-'),
  RENAMED('R'),
  UNKNOWN('?'),
  NONEXISTENT('X'),
  CONFLICTED('C'),
  PENDING_MERGE('P'),
  ADDED('N'),
  DELETED('D'),
  KIND_CHANGED('K'),
  MODIFIED('M'),
  EXECUTE_BIT_CHANGED('*'),
  IGNORED('I');

  private final char id;

  private BzrFileStatusEnum(char id) {
    this.id = id;
  }

  public static BzrFileStatusEnum valueOf(char c) {
    for (BzrFileStatusEnum status : BzrFileStatusEnum.values()) {
      if (status.id == c) {
        return status;
      }
    }
    return null;
  }
}
