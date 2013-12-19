package bazaar4idea;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.emergent.bzr4j.core.BazaarRevision;
import org.emergent.bzr4j.core.utils.NaturalOrderComparator;
import org.jetbrains.annotations.NotNull;

public final class BzrRevisionNumber implements VcsRevisionNumber {

  private String m_rev;

  public static BzrRevisionNumber createBzrRevisionNumber(BazaarRevision rev) {
    return new BzrRevisionNumber(rev.toString());
  }

  public static BzrRevisionNumber getInstance(String revision, String changeset) {
    return new BzrRevisionNumber(revision);
  }

  public static BzrRevisionNumber getLocalInstance(String revision) {
    return new BzrRevisionNumber(revision);
  }

  private BzrRevisionNumber(@NotNull String rev) {
    m_rev = rev;
//    if (rev.length() > 0 && rev.indexOf(':') < 0) {
//      m_rev = "revno:" + m_rev;
//    }
  }

  public String asString() {
    return m_rev;
  }

  public int compareTo(VcsRevisionNumber o) {
    if (this == o) {
      return 0;
    }

    if (!(o instanceof BzrRevisionNumber)) {
      return -1;
    }

    return NaturalOrderComparator.compareObjects(asString(), o.asString());
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(m_rev)
        .toHashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof BzrRevisionNumber)) {
      return false;
    }
    BzrRevisionNumber that = (BzrRevisionNumber)object;
    return compareTo(that) == 0;
  }
}
