package bazaar4idea;

import bazaar4idea.command.BzrCommand;
import bazaar4idea.command.BzrSimpleHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.emergent.bzr4j.core.BazaarRevision;
import org.emergent.bzr4j.core.utils.NaturalOrderComparator;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.StringTokenizer;

public final class BzrRevisionNumber implements VcsRevisionNumber {

  private static final Logger LOG = Logger.getInstance(BzrRevisionNumber.class);

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

  public BzrRevisionNumber(@NonNls @NotNull String version) {
    m_rev = version;
//    if (rev.length() > 0 && rev.indexOf(':') < 0) {
//      m_rev = "revno:" + m_rev;
//    }
  }

  /**
   * A constructor from version and time
   *
   * @param version   the version number
   * @param timeStamp the time when the version has been created
   */
  public BzrRevisionNumber(@NotNull String version, @NotNull Date timeStamp) {
    // TODO
//    myTimestamp = timeStamp;
    m_rev = version;
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


  /**
   * Resolve revision number for the specified revision
   *
   * @param project a project
   * @param vcsRoot a vcs root
   * @param rev     a revision expression
   * @return a resolved revision number with correct time
   * @throws VcsException if there is a problem with running git
   */
  public static BzrRevisionNumber resolve(Project project, VirtualFile vcsRoot, @NonNls String rev)
          throws VcsException {
    BzrSimpleHandler h = new BzrSimpleHandler(project, vcsRoot, BzrCommand.REV_LIST);
    h.setSilent(true);
    // TODO crosscheck git flags with bzr
    h.addParameters("--timestamp", "--max-count=1", rev);
    h.endOptions();
    final String output = h.run();
    return parseRevlistOutputAsRevisionNumber(h, output);
  }

  @NotNull
  public static BzrRevisionNumber parseRevlistOutputAsRevisionNumber(@NotNull BzrSimpleHandler h,
                                                                     @NotNull String output) {
    StringTokenizer tokenizer = new StringTokenizer(output, "\n\r \t", false);
    LOG.assertTrue(tokenizer.hasMoreTokens(), "No required tokens in the output: \n" + output);
    Date timestamp = BzrUtil.parseTimestampWithNFEReport(tokenizer.nextToken(), h, output);
    return new BzrRevisionNumber(tokenizer.nextToken(), timestamp);
  }

}
