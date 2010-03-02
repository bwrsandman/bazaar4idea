package org.emergent.bzr4j.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.emergent.bzr4j.core.utils.StringUtil;
import org.emergent.bzr4j.intellij.command.BzrCatCommand;

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
  private byte[] content;

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

  public void loadContent() throws VcsException {
    try {
      Charset charset = m_file.toFilePath().getCharset();
      String result = new BzrCatCommand(m_project).execute(m_file, m_revisionNumber, charset);
      if (result == null) {
        content = new byte[0];
      } else {
        content = result.getBytes(charset.name());
      }
    } catch (UnsupportedEncodingException e) {
      throw new VcsException(e);
    }
  }

  public byte[] getContent() throws IOException {
    return content;
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
}
