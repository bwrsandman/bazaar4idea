package bazaar4idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Throwable2Computable;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.impl.ContentRevisionCache;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.emergent.bzr4j.core.utils.StringUtil;
import bazaar4idea.command.BzrCatCommand;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Date;

public class BzrFileRevision implements VcsFileRevision {

  private final Project m_project;
  private final BzrFile m_file;
  private final BzrRevisionNumber m_revisionNumber;
  private final String branchName;
  private final Date revisionDate;
  private final String author;
  private final String commitMessage;

  public BzrFileRevision(Project project, BzrFile file, BzrRevisionNumber revisionNumber,
      String branchName, String revisionDate, String author, String commitMessage) throws ParseException {
    this(project, file, revisionNumber, branchName, StringUtil.parseLogDate(revisionDate), author, commitMessage);
  }

  public BzrFileRevision(Project project, BzrFile file, BzrRevisionNumber revisionNumber,
      String branchName, Date revisionDate, String author, String commitMessage) {
    m_project = project;
    m_file = file;
    m_revisionNumber = revisionNumber;
    this.branchName = branchName;
    this.revisionDate = revisionDate;
    this.author = author;
    this.commitMessage = commitMessage;
  }

  public BzrRevisionNumber getRevisionNumber() {
    return m_revisionNumber;
  }

  public String getBranchName() {
    return branchName;
  }

  public Date getRevisionDate() {
    return revisionDate;
  }

  public String getAuthor() {
    return author;
  }

  public String getCommitMessage() {
    return commitMessage;
  }

  public byte[] loadContent() throws VcsException {
    try {
      Charset charset = m_file.toFilePath().getCharset();
      String result = new BzrCatCommand(m_project).execute(m_file, m_revisionNumber, charset);
      if (result == null) {
        return new byte[0];
      } else {
        return result.getBytes(charset.name());
      }
    } catch (UnsupportedEncodingException e) {
      throw new VcsException(e);
    }
  }

  public byte[] getContent() throws IOException, VcsException {
    return ContentRevisionCache.getOrLoadAsBytes(m_project, m_file.toFilePath(), getRevisionNumber(), BzrVcs.getKey(),
        ContentRevisionCache.UniqueType.REPOSITORY_CONTENT,
        new Throwable2Computable<byte[], VcsException, IOException>() {
          public byte[] compute() throws VcsException, IOException {
            return loadContent();
          }
        });
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(m_file)
        .append(m_revisionNumber)
        .toHashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof BzrFileRevision)) {
      return false;
    }
    BzrFileRevision that = (BzrFileRevision)object;
    return new EqualsBuilder()
        .append(m_file, that.m_file)
        .append(m_revisionNumber, that.m_revisionNumber)
        .isEquals();
  }

  @Override
  public RepositoryLocation getChangedRepositoryPath() {
    return null;  // use initial url..
  }
}
