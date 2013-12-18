/*
 * Copyright (c) 2009 Patrick Woodworth
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
package bazaar4idea;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.vfs.AbstractVcsVirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.emergent.bzr4j.core.utils.BzrCoreUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Patrick Woodworth
 */
public class BzrUtil {

  private static final Logger LOG = Logger.getInstance(BzrUtil.class.getName());

  private static final Map<String,File> sm_rootFileCache = new WeakHashMap<String, File>();

  /**
   * Return a bzr root for the file path (the parent directory with ".bzr" subdirectory)
   *
   * @param filePath a file path
   * @return bzr root for the file or null if the file is not under bzr
   */
  @Nullable
  private static VirtualFile getBzrRootOrNull(final FilePath filePath) {
    File file = getBzrRootOrNull(filePath.getIOFile());
    if (file == null) {
      return null;
    }
    return LocalFileSystem.getInstance().findFileByIoFile(file);
  }

  /**
   * Return a bzr root for the file path (the parent directory with ".bzr" subdirectory)
   *
   * @param file a file path
   * @return bzr root for the file or null if the file is not under bzr
   */
  @Nullable
  public static File getBzrRootOrNull(File file) {
    while (file != null && (!file.exists() || !file.isDirectory() || !((new File(file, ".bzr/branch")).isDirectory()))) {
      file = file.getParentFile();
    }
    return file;
  }

  /**
   * Return a bzr root for the file (the parent directory with ".bzr" subdirectory)
   *
   * @param file the file to check
   * @return bzr root for the file or null if the file is not not under Bzr
   */
  @Nullable
  public static VirtualFile bzrRootOrNull(final VirtualFile file) {
    if (file instanceof AbstractVcsVirtualFile) {
      return getBzrRootOrNull(VcsUtil.getFilePath(file.getPath()));
    }
    VirtualFile root = file;
    while (root != null) {
      if (root.findFileByRelativePath(".bzr/branch") != null) {
        return root;
      }
      root = root.getParent();
    }
    return root;
  }

  public static File findBzrRoot(File file) {
    return findBzrRoot(file, true);
  }

  public static File findBzrRoot(File file, boolean useCache) {
    if (file == null)
      return null;

    String filePath = file.getAbsolutePath();
    synchronized (sm_rootFileCache) {
      File rootFile = useCache ? sm_rootFileCache.get(filePath) : null;
      if (rootFile == null) {
        rootFile = BzrCoreUtil.getBzrRoot(file);
        if (rootFile != null) {
          sm_rootFileCache.put(filePath, rootFile);
        }
      }
      return rootFile;
    }
  }

  /**
   * Get relative path
   *
   * @param root a root path
   * @param path a path to file (possibly deleted file)
   * @return a relative path
   * @throws IllegalArgumentException if path is not under root.
   */
  public static String relativePath(final File root, FilePath path) {
    return relativePath(root, path.getIOFile());
  }

  /**
   * Get relative path
   *
   * @param root a root path
   * @param file a virtual file
   * @return a relative path
   * @throws IllegalArgumentException if path is not under root.
   */
  public static String relativePath(final File root, VirtualFile file) {
    return relativePath(root, VfsUtil.virtualToIoFile(file));
  }


  /**
   * Get relative path
   *
   * @param root a root path
   * @param path a path to file (possibly deleted file)
   * @return a relative path
   * @throws IllegalArgumentException if path is not under root.
   */
  public static String relativePath(final File root, File path) {
    return BzrCoreUtil.relativePath(root, path);
  }




  private static class UnusedUtil {

//    /**
//     * Return a bzr root for the file path (the parent directory with ".bzr" subdirectory)
//     *
//     * @param filePath a file path
//     * @return bzr root for the file
//     * @throws IllegalArgumentException if the file is not under bzr
//     * @throws com.intellij.openapi.vcs.VcsException             if the file is not under bzr
//     */
//    private static VirtualFile getBzrRoot(final FilePath filePath) throws VcsException {
//      VirtualFile root = getBzrRootOrNull(filePath);
//      if (root != null) {
//        return root;
//      }
//      throw new VcsException("The file " + filePath + " is not under bzr.");
//    }
//
//
//    /**
//     * Return a bzr root for the file (the parent directory with ".bzr" subdirectory)
//     *
//     * @param file the file to check
//     * @return bzr root for the file
//     * @throws VcsException if the file is not under bzr
//     */
//    public static VirtualFile getBzrRoot(@NotNull final VirtualFile file) throws VcsException {
//      final VirtualFile root = bzrRootOrNull(file);
//      if (root != null) {
//        return root;
//      } else {
//        throw new VcsException("The file " + file.getPath() + " is not under bzr.");
//      }
//    }

//    /**
//     * Check if the virtual file under bzr
//     *
//     * @param vFile a virtual file
//     * @return true if the file is under bzr
//     */
//    private static boolean isUnderBzr(final VirtualFile vFile) {
//      return bzrRootOrNull(vFile) != null;
//    }
//
//    /**
//     * Get relative path
//     *
//     * @param root a root path
//     * @param path a path to file (possibly deleted file)
//     * @return a relative path
//     * @throws IllegalArgumentException if path is not under root.
//     */
//    public static String relativePath(final VirtualFile root, FilePath path) {
//      return relativePath(VfsUtil.virtualToIoFile(root), path.getIOFile());
//    }
//

//    /**
//     * Get relative path
//     *
//     * @param root a root file
//     * @param file a virtual file
//     * @return a relative path
//     * @throws IllegalArgumentException if path is not under root.
//     */
//    public static String relativePath(final VirtualFile root, VirtualFile file) {
//      return relativePath(VfsUtil.virtualToIoFile(root), VfsUtil.virtualToIoFile(file));
//    }
//
//    public static FilePath toFilePath(VirtualFile vf) {
//      return VcsUtil.getFilePath(vf.getPath());
//    }
//
//    public static FilePath toFilePath(File vf) {
//      return VcsUtil.getFilePath(vf);
//    }
//
//    public static BazaarRoot toBranchLocation(File file) {
//      return BazaarRoot.createRootLocation(file);
//    }
//
//    public static BazaarRoot toBranchLocation(VirtualFile file) {
//      return BazaarRoot.createRootLocation(VfsUtil.virtualToIoFile(file));
//    }
//
//    public static BazaarRoot toBranchLocation(FilePath filePath) {
//      return toBranchLocation(filePath.getIOFile());
//    }
//
//    @Nullable
//    private static BzrVcs getBzrVcs(VirtualFile file) {
//      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
//        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
//        if (vcs instanceof BzrVcs) {
//          LOG.debug(String.format("Found BzrVcs for file %s: %s", file, vcs));
//          return (BzrVcs)vcs;
//        }
//      }
//      return null;
//    }
//
//    private static void refreshFiles(List<VirtualFile> myFilesToRefresh, final Project project,
//        boolean async) {
//      final List<VirtualFile> toRefreshFiles = new ArrayList<VirtualFile>();
//      final List<VirtualFile> toRefreshDirs = new ArrayList<VirtualFile>();
//      for (VirtualFile file : myFilesToRefresh) {
//        if (file.isDirectory()) {
//          LOG.debug("Gonna refresh: " + file.getName());
//          toRefreshDirs.add(file);
//        } else {
//          LOG.debug("Gonna refresh: " + file.getName());
//          toRefreshFiles.add(file);
//        }
//      }
//      // if refresh asynchronously, local changes would also be notified that they are dirty asynchronously,
//      // and commit could be executed while not all changes are visible
//      final RefreshSession session =
//          RefreshQueue.getInstance().createSession(async, true, new Runnable() {
//            public void run() {
//              if (project.isDisposed()) return;
//              filterOutInvalid(toRefreshFiles);
//              filterOutInvalid(toRefreshDirs);
//
//              final VcsDirtyScopeManager vcsDirtyScopeManager =
//                  VcsDirtyScopeManager.getInstance(project);
//              vcsDirtyScopeManager.filesDirty(toRefreshFiles, toRefreshDirs);
//            }
//          });
//      session.addAllFiles(myFilesToRefresh);
//      session.launch();
//    }
//
//    private static void filterOutInvalid(final Collection<VirtualFile> files) {
//      for (Iterator<VirtualFile> iterator = files.iterator(); iterator.hasNext();) {
//        final VirtualFile file = iterator.next();
//        if (!file.isValid()) {
//          LOG.info("Refresh root is not valid: " + file.getPath());
//          iterator.remove();
//        }
//      }
//    }
//
//    private static boolean isVersioned(BzrVcs bzr, VirtualFile file) throws BazaarException {
//      FileStatusManager statMgr = FileStatusManager.getInstance(bzr.getProject());
//      final FileStatus fileStatus = statMgr.getStatus(file);
//      return fileStatus != FileStatus.UNKNOWN;
//    }
//
//    private static boolean isIgnored(BzrVcs bzr, VirtualFile file) throws BazaarException {
//      FileStatusManager statMgr = FileStatusManager.getInstance(bzr.getProject());
//      final FileStatus fileStatus = statMgr.getStatus(file);
//      return fileStatus == FileStatus.UNKNOWN;
//    }
//
//    private static String getCommonParent(Collection<FilePath> col) {
//      String retval = null;
//      for (FilePath path : col) {
//        String next = path.getPath();
//        if (retval != null) {
//          IOUtil.getCommonParent(retval, next);
//        } else {
//          retval = next;
//        }
//      }
//      return retval;
//    }
//
//    /**
//     * Find longest common prefix of two strings.
//     */
//    private static String longestCommonPrefix(String s, String t) {
//      return s.substring(0, longestCommonPrefixLength(s, t));
//    }
//
//    private static int longestCommonPrefixLength(String s, String t) {
//      int m = Math.min(s.length(), t.length());
//      for (int k = 0; k < m; ++k)
//        if (s.charAt(k) != t.charAt(k))
//          return k;
//      return m;
//    }
  }
}
