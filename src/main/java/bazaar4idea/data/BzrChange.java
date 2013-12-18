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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import bazaar4idea.BzrFile;

import java.util.EnumSet;

public final class BzrChange {

  private BzrFile beforeFile;
  private BzrFile afterFile;
  private EnumSet<BzrFileStatusEnum> status;

  public BzrChange(BzrFile hgFile, EnumSet<BzrFileStatusEnum> status) {
    this.beforeFile = hgFile;
    this.afterFile = hgFile;
    this.status = status;
  }

  public BzrFile beforeFile() {
    return beforeFile;
  }

  public BzrFile afterFile() {
    return afterFile;
  }

  public EnumSet<BzrFileStatusEnum> getStatus() {
    return status;
  }

  public void setBeforeFile(BzrFile beforeFile) {
    this.beforeFile = beforeFile;
  }

  public void setAfterFile(BzrFile afterFile) {
    this.afterFile = afterFile;
  }

  public void setStatus(EnumSet<BzrFileStatusEnum> status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof BzrChange)) {
      return false;
    }
    BzrChange that = (BzrChange)object;
    return new EqualsBuilder()
        .append(beforeFile, that.beforeFile)
        .append(afterFile, that.afterFile)
        .append(status, that.status)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(beforeFile)
        .append(afterFile)
        .append(status)
        .toHashCode();
  }

}
