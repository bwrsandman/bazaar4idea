package bazaar4idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import bazaar4idea.command.BzrCatCommand;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Patrick Woodworth
 */
public class BzrContentRevision implements ContentRevision {

  private static final Logger LOG = Logger.getInstance(BzrContentRevision.class.getName());

  private final Project m_project;

  private final FilePath m_filePath;

  private final BzrRevisionNumber m_revisionNumber;

  private String m_content;

  private BzrContentRevision(Project project, FilePath path, BzrRevisionNumber revision) {
    m_project = project;
    m_filePath = path;
    m_revisionNumber = revision;
  }

  public static BzrContentRevision createBzrContentRevision(
      Project project, VirtualFile vcsRoot, VirtualFile file, BzrRevisionNumber revision) {
    return createBzrContentRevision(project, vcsRoot, VfsUtil.virtualToIoFile(file), revision);
  }

  public static BzrContentRevision createBzrContentRevision(
      Project project, VirtualFile vcsRoot, final File file, BzrRevisionNumber revision) {
    FilePath filePath = getFilePath(file);
    return new BzrContentRevision(project, filePath, revision);
  }

  public static BzrContentRevision createBzrContentRevision(
      Project project, VirtualFile vcsRoot, final FilePath filePath, BzrRevisionNumber revision) {
    return new BzrContentRevision(project, filePath, revision);
  }

  public String getContent() throws VcsException {
    if (StringUtils.isBlank(m_content)) {
      FilePath fpath = getFile();
      if (fpath.isNonLocal()) {
        LOG.debug("nonLocal: " + fpath);
      }
      if (fpath.isDirectory()) {
        return null;
      }
      m_content = new BzrCatCommand(m_project).execute(fpath.getIOFile(), m_revisionNumber, fpath.getCharset());
    }
    return m_content;
  }

  @NotNull
  public FilePath getFile() {
    return m_filePath;
  }

  @NotNull
  public BzrRevisionNumber getRevisionNumber() {
    return m_revisionNumber;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(getFile())
        .append(m_revisionNumber)
        .toHashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof BzrContentRevision)) {
      return false;
    }
    BzrContentRevision that = (BzrContentRevision)object;
    return new EqualsBuilder()
        .append(getFile(), that.getFile())
        .append(m_revisionNumber, that.m_revisionNumber)
        .isEquals();
  }

  private static FilePath getFilePath(final File ioFile) {
    return ApplicationManager.getApplication().runReadAction(
        new Computable<FilePath>() {
          public FilePath compute() {
            return VcsUtil.getFilePath(ioFile);
          }
        });
  }
}
