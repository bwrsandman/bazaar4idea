package bazaar4idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import bazaar4idea.commands.BzrCatCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;

/**
 * @author Patrick Woodworth
 */
public class BzrContentRevision implements ContentRevision {

  private static final Logger LOG = Logger.getInstance(BzrContentRevision.class.getName());

  @NotNull protected final Project myProject;

  @NotNull protected final FilePath myFile;

  @NotNull protected final BzrRevisionNumber myRevision;

  private String myContent;
  /**
   * The charset for the file
   */
  @Nullable private Charset myCharset;


  protected BzrContentRevision(@NotNull FilePath path, @NotNull BzrRevisionNumber revision,
                             @NotNull Project project, @Nullable Charset charset) {
    myProject = project;
    myFile = path;
    myRevision = revision;
    myCharset = charset;
  }

  /**
   * Create revision
   *
   *
   * @param vcsRoot        a vcs root for the repository
   * @param path           an path inside with possibly escape sequences
   * @param revisionNumber a revision number, if null the current revision will be created
   * @param project        the context project
   * @param isDeleted      if true, the file is deleted
   * @param unescapePath
   * @return a created revision
   * @throws VcsException
   *          if there is a problem with creating revision
   */
  public static ContentRevision createRevision(VirtualFile vcsRoot,
                                               String path,
                                               @Nullable VcsRevisionNumber revisionNumber,
                                               Project project,
                                               boolean isDeleted, final boolean canBeDeleted,
                                               boolean unescapePath) throws VcsException {
    final FilePath file;
    if (project.isDisposed()) {
      file = new FilePathImpl(new File(makeAbsolutePath(vcsRoot, path, unescapePath)), false);
    } else {
      file = createPath(vcsRoot, path, isDeleted, canBeDeleted, unescapePath);
    }
    return createRevision(file, revisionNumber, project);
  }

  public static ContentRevision createRevision(@NotNull final VirtualFile file,
                                               @Nullable final VcsRevisionNumber revisionNumber,
                                               @NotNull final Project project) {
    return createRevision(file, revisionNumber, project, null);
  }

  public static ContentRevision createRevision(@NotNull final VirtualFile file,
                                               @Nullable final VcsRevisionNumber revisionNumber,
                                               @NotNull final Project project, @Nullable final Charset charset) {
    final FilePathImpl filePath = new FilePathImpl(file);
    return createRevision(filePath, revisionNumber, project, charset);
  }

  public static ContentRevision createRevision(@NotNull final FilePath filePath,
                                               @Nullable final VcsRevisionNumber revisionNumber,
                                               @NotNull final Project project, @Nullable final Charset charset) {
    if (revisionNumber != null && revisionNumber != VcsRevisionNumber.NULL) {
      return createRevisionImpl(filePath, (BzrRevisionNumber)revisionNumber, project, charset);
    }
    else {
      return CurrentContentRevision.create(filePath);
    }
  }


  private static ContentRevision createRevision(@NotNull FilePath filePath,
                                                @Nullable VcsRevisionNumber revisionNumber,
                                                @NotNull Project project) {
    if (revisionNumber != null && revisionNumber != VcsRevisionNumber.NULL) {
      return createRevisionImpl(filePath, (BzrRevisionNumber)revisionNumber, project, null);
    }
    else {
      return CurrentContentRevision.create(filePath);
    }
  }


  public static BzrContentRevision createRevision(Project project,
                                                  VirtualFile vcsRoot,
                                                  VirtualFile file,
                                                  BzrRevisionNumber revision) {
    return createRevision(project, vcsRoot, VfsUtil.virtualToIoFile(file), revision);
  }

  public static BzrContentRevision createRevision(Project project,
                                                  VirtualFile vcsRoot,
                                                  final File file,
                                                  BzrRevisionNumber revision) {
    FilePath filePath = getFilePath(file);
    return new BzrContentRevision(filePath, revision, project, null);
  }

  public static BzrContentRevision createRevision(Project project,
                                                  VirtualFile vcsRoot,
                                                  final FilePath filePath,
                                                  BzrRevisionNumber revision) {
    return new BzrContentRevision(filePath, revision, project, null);
  }

  public String getContent() throws VcsException {
    if (StringUtils.isBlank(myContent)) {
      FilePath fpath = getFile();
      if (fpath.isNonLocal()) {
        LOG.debug("nonLocal: " + fpath);
      }
      if (fpath.isDirectory()) {
        return null;
      }
      myContent = new BzrCatCommand(myProject).execute(fpath.getIOFile(), myRevision, fpath.getCharset());
    }
    return myContent;
  }

  @NotNull
  public FilePath getFile() {
    return myFile;
  }

  @NotNull
  public BzrRevisionNumber getRevisionNumber() {
    return myRevision;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(getFile())
        .append(myRevision)
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
        .append(myRevision, that.myRevision)
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

  public static ContentRevision createRevisionForTypeChange(@NotNull Project project,
                                                            @NotNull VirtualFile vcsRoot,
                                                            @NotNull String path,
                                                            @Nullable VcsRevisionNumber revisionNumber,
                                                            boolean unescapePath) throws VcsException {
    final FilePath filePath;
    if (revisionNumber == null) {
      File file = new File(makeAbsolutePath(vcsRoot, path, unescapePath));
      VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      filePath = virtualFile == null ? new FilePathImpl(file, false) : new FilePathImpl(virtualFile);
    } else {
      filePath = createPath(vcsRoot, path, false, false, unescapePath);
    }
    return createRevision(filePath, revisionNumber, project);
  }

  public static FilePath createPath(@NotNull VirtualFile vcsRoot, @NotNull String path,
                                    boolean isDeleted, boolean canBeDeleted, boolean unescapePath) throws VcsException {
    final String absolutePath = makeAbsolutePath(vcsRoot, path, unescapePath);
    FilePath file = isDeleted ? VcsUtil.getFilePathForDeletedFile(absolutePath, false) : VcsUtil.getFilePath(absolutePath, false);
    if (canBeDeleted && (! SystemInfo.isFileSystemCaseSensitive) && VcsUtil.caseDiffers(file.getPath(), absolutePath)) {
      // as for deleted file
      file = FilePathImpl.createForDeletedFile(new File(absolutePath), false);
    }
    return file;
  }

  private static String makeAbsolutePath(@NotNull VirtualFile vcsRoot, @NotNull String path, boolean unescapePath) throws VcsException {
    final String unescapedPath = /*unescapePath ? GitUtil.unescapePath(path) :*/ path;
    return vcsRoot.getPath() + "/" + unescapedPath;
  }

  private static BzrContentRevision createRevisionImpl(@NotNull FilePath path,
                                                       @NotNull BzrRevisionNumber revisionNumber,
                                                       @NotNull Project project,
                                                       @Nullable final Charset charset) {
    if (path.getFileType().isBinary()) {
      return new BzrBinaryContentRevision(path, revisionNumber, project);
    } else {
      return new BzrContentRevision(path, revisionNumber, project, charset);
    }
  }

}
