package org.emergent.bzr4j.intellij.ui;

import com.intellij.ProjectTopics;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandAdapter;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileManagerListener;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.emergent.bzr4j.core.utils.BzrCoreUtil;
import org.emergent.bzr4j.intellij.BzrProjectSettings;
import org.emergent.bzr4j.intellij.BzrRootsListener;
import org.emergent.bzr4j.intellij.BzrUtil;
import org.emergent.bzr4j.intellij.BzrVcs;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The component tracks Bzr roots for the project. If roots are mapped incorrectly it
 * shows balloon that notifies user about the problem and offers to correct root mapping.
 */
public class BzrRootTracker implements VcsListener {

  private static final Logger LOG = Logger.getInstance(BzrRootTracker.class.getName());

  /**
   * The context project
   */
  private final Project myProject;
  /**
   * Tracker of roots for project root manager
   */
  private final ProjectRootManager myProjectRoots;
  /**
   * The vcs manager that tracks content roots
   */
  private final ProjectLevelVcsManager myVcsManager;
  /**
   * The vcs instance
   */
  private final BzrVcs myVcs;
  /**
   * If true, the tracking is enabled.
   */
  private final AtomicBoolean myIsEnabled = new AtomicBoolean(false);
  /**
   * If true, the root configuration has been possibly invalidated
   */
  private final AtomicBoolean myRootsInvalidated = new AtomicBoolean(true);
  /**
   * If true, there are some configured git roots, or listener has never been run yet
   */
  private final AtomicBoolean myHasBzrRoots = new AtomicBoolean(true);
  /**
   * If true, the notification is currently active and has not been dismissed yet.
   */
  private final AtomicBoolean myNotificationPosted = new AtomicBoolean(false);

  private final AtomicReference<FixedRoots> m_cachedFixes = new AtomicReference<FixedRoots>();    

  private final MergingUpdateQueue myQueue;

  private Notification myNotification;

  /**
   * The invalid git roots
   */
  private static final String BZR_INVALID_ROOTS_ID = "Bazaar";

  /**
   * The command listener
   */
  private CommandListener myCommandListener;
  /**
   * The file listener
   */
  private MyFileListener myFileListener;
  /**
   * Listener for refresh events
   */
  private VirtualFileManagerListener myVirtualFileManagerListener;
  /**
   * Local file system service
   */
  private LocalFileSystem myLocalFileSystem;
  /**
   * The multicaster for root events
   */
  private BzrRootsListener myMulticaster;

  private final MessageBusConnection myMessageBusConnection;

  /**
   * The constructor
   *
   * @param project     the project instance
   * @param multicaster the listeners to notify
   */
  public BzrRootTracker(BzrVcs vcs, @NotNull Project project, @NotNull BzrRootsListener multicaster) {

    myMulticaster = multicaster;
    if (project.isDefault()) {
      throw new IllegalArgumentException("The project must not be default");
    }
    myProject = project;
    myProjectRoots = ProjectRootManager.getInstance(myProject);
    myQueue = new MergingUpdateQueue("queue", 500, true, null, project, null, false);
    myVcs = vcs;
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
    myVcsManager.addVcsListener(this);
    myLocalFileSystem = LocalFileSystem.getInstance();
    myMessageBusConnection = myProject.getMessageBus().connect();
    myMessageBusConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      public void beforeRootsChange(ModuleRootEvent event) {
        // do nothing
      }

      public void rootsChanged(ModuleRootEvent event) {
        invalidate();
      }
    });
    myCommandListener = new CommandAdapter() {
      @Override
      public void commandFinished(CommandEvent event) {
        if (!myRootsInvalidated.compareAndSet(true, false)) {
          return;
        }
        scheduleRootsCheck(false);
      }
    };
    CommandProcessor.getInstance().addCommandListener(myCommandListener);
    myFileListener = new MyFileListener();
    VirtualFileManager fileManager = VirtualFileManager.getInstance();
    fileManager.addVirtualFileListener(myFileListener);
    myVirtualFileManagerListener = new VirtualFileManagerListener() {
      public void beforeRefreshStart(boolean asynchonous) {
      }

      public void afterRefreshFinish(boolean asynchonous) {
        if (!myRootsInvalidated.compareAndSet(true, false)) {
          return;
        }
        scheduleRootsCheck(false);
      }
    };
    fileManager.addVirtualFileManagerListener(myVirtualFileManagerListener);
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(new Runnable() {
      public void run() {
        myIsEnabled.set(true);
        scheduleRootsCheck(true);
      }
    });
  }

  /**
   * Dispose the component removing all related listeners
   */
  public void dispose() {
    myVcsManager.removeVcsListener(this);
    myMessageBusConnection.disconnect();
    CommandProcessor.getInstance().removeCommandListener(myCommandListener);
    VirtualFileManager fileManager = VirtualFileManager.getInstance();
    fileManager.removeVirtualFileListener(myFileListener);
    fileManager.removeVirtualFileManagerListener(myVirtualFileManagerListener);
  }

  /**
   * {@inheritDoc}
   */
  public void directoryMappingChanged() {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        scheduleRootsCheck(true);
      }
    });
  }

  private void scheduleRootsCheck(final boolean rootsChanged) {
    if (ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isHeadlessEnvironment()) {
      doCheckRoots(rootsChanged);
      return;
    }
    myQueue.queue(new Update("root check") {
      public void run() {
        if (myProject.isDisposed()) return;
        doCheckRoots(rootsChanged);
      }
    });
  }

  /**
   * Check roots for changes.
   *
   * @param rootsChanged
   */
  private void doCheckRoots(boolean rootsChanged) {
    if (!myIsEnabled.get()
        || !BzrProjectSettings.getInstance(myProject).isRootCheckingEnabled()
        || (!rootsChanged && !myHasBzrRoots.get())) {
      clearRootsNotification();
      return;
    }

    final HashSet<VirtualFile> rootSet = new HashSet<VirtualFile>();
    boolean hasInvalidRoots = ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
      public Boolean compute() {
        for (VcsDirectoryMapping m : myVcsManager.getDirectoryMappings()) {
          if (!m.getVcs().equals(myVcs.getName())) {
            continue;
          }
          String path = m.getDirectory();
          if (path.length() == 0) {
            VirtualFile baseDir = myProject.getBaseDir();
            assert baseDir != null;
            path = baseDir.getPath();
          }
          VirtualFile root = lookupFile(path);
          if (root == null || rootSet.contains(root)) {
            return true;
          }
          if (!root.equals(lookupFileRoot(path))) {
            LOG.debug(String.format("has another root \"%s\"", path));
            return true;
          }
          rootSet.add(root);
        }
        return false;
      }
    });
    if (!hasInvalidRoots && rootSet.isEmpty()) {
      myHasBzrRoots.set(false);
      return;
    } else {
      myHasBzrRoots.set(true);
    }

    if (!hasInvalidRoots) {
      // check if roots have a problem
      for (final VirtualFile root : rootSet) {
        hasInvalidRoots = hasUnmappedSubroots(root, rootSet);
        if (hasInvalidRoots) {
          break;
        }
      }
    }

    if (!hasInvalidRoots) {
      // all roots are correct
      clearRootsNotification();
    }
    else if (myNotificationPosted.compareAndSet(false, true)) {
      final List<VcsDirectoryMapping> vcsDirectoryMappings = new ArrayList<VcsDirectoryMapping>(myVcsManager.getDirectoryMappings());
      calculateNewRoots(vcsDirectoryMappings);
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        public void run() {
          myNotification = new Notification(BZR_INVALID_ROOTS_ID,
              BzrVcsMessages.message("root.tracker.message.title"),
              BzrVcsMessages.message("root.tracker.message"),
              NotificationType.ERROR,
              new NotificationListener() {
                public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                  if (fixRoots()) {
                    notification.expire();
                  }
                }
              });

          Notifications.Bus.notify(myNotification, myProject);
        }
      });
    }
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      public void run() {
        myMulticaster.gitRootsChanged();
      }
    });
  }

  private void clearRootsNotification() {
    if (myNotificationPosted.compareAndSet(true, false)) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        public void run() {
          if (myNotification != null) {
            if (!myNotification.isExpired()) {
              myNotification.expire();
            }
            myNotification = null;
          }
        }
      });
    }
  }

  /**
   * Check if there are some unmapped subdirectories under git
   *
   * @param directory the content root to check
   * @param rootSet   the mapped root set
   */
  private static boolean hasUnmappedSubroots(final VirtualFile directory, final @NotNull HashSet<VirtualFile> rootSet) {
    VirtualFile[] children = ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile[]>() {
      public VirtualFile[] compute() {
        return directory.isValid() ? directory.getChildren() : VirtualFile.EMPTY_ARRAY;
      }
    });

    for (final VirtualFile child : children) {
      if (!child.isDirectory()) {
        continue;
      }
      if (child.getName().equals(".bzr") && child.findChild("branch") != null) {
        return !rootSet.contains(child.getParent());
      }
      if (hasUnmappedSubroots(child, rootSet)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Calculate fix mapped roots
   * @param vcsDirectoryMappings
   */
  private FixedRoots calculateNewRoots(final List<VcsDirectoryMapping> vcsDirectoryMappings) {
    final VirtualFile baseDir = myProject.getBaseDir();
    assert baseDir != null;

    FixedRoots fixedRoots = m_cachedFixes.get();
    if (fixedRoots != null && fixedRoots.m_rootsHashCode == vcsDirectoryMappings.hashCode()) {
      return fixedRoots;
    }

    if (fixedRoots != null && fixedRoots.m_rootsHashCode != vcsDirectoryMappings.hashCode()) {
      LOG.debug("outdated hashcode");
    }

    fixedRoots = new FixedRoots(vcsDirectoryMappings.hashCode());

    final HashSet<String> mapped = fixedRoots.mapped;
    final HashSet<String> removed = fixedRoots.removed;
    final HashSet<String> added = fixedRoots.added;

    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        for (Iterator<VcsDirectoryMapping> i = vcsDirectoryMappings.iterator(); i.hasNext();) {
          VcsDirectoryMapping m = i.next();
          String vcsName = myVcs.getName();
          if (!vcsName.equals(m.getVcs())) {
            continue;
          }
          String path = m.getDirectory();
          if (path.length() == 0) {
            path = baseDir.getPath();
          }
          VirtualFile file = lookupFile(path);
          if (file != null && !mapped.add(file.getPath())) {
            // eliminate duplicates
            i.remove();
            continue;
          }
          VirtualFile realRoot1 = BzrUtil.bzrRootOrNull(file);
          VirtualFile realRoot2 = lookupFileRoot(path);
          if (file == null || realRoot1 == null) {
            removed.add(path);
          }
          if (file != null && realRoot2 != null && !realRoot2.equals(file)) {
            removed.add(path);
            added.add(realRoot2.getPath());
          }
        }
        for (String m : mapped) {
          VirtualFile file = lookupFile(m);
          if (file == null) {
            continue;
          }
          addSubroots(file, added, mapped);
          if (removed.contains(m)) {
            continue;
          }
          VirtualFile root = BzrUtil.bzrRootOrNull(file);
          assert root != null;
          for (String o : mapped) {
            // the mapped collection is not modified here, so order is being kept
            if (o.equals(m) || removed.contains(o)) {
              continue;
            }
            if (o.startsWith(m)) {
              VirtualFile otherFile = lookupFile(m);
              assert otherFile != null;
              VirtualFile otherRoot = BzrUtil.bzrRootOrNull(otherFile);
              assert otherRoot != null;
              if (otherRoot == root) {
                removed.add(o);
              }
              else if (otherFile != otherRoot) {
                added.add(otherRoot.getPath());
                removed.add(o);
              }
            }
          }
        }
      }
    });

    m_cachedFixes.set(fixedRoots);
    return fixedRoots;
  }

  /**
   * Fix mapped roots
   *
   * @return true if roots now in the correct state
   */
  boolean fixRoots() {
    final VirtualFile baseDir = myProject.getBaseDir();
    assert baseDir != null;

    final List<VcsDirectoryMapping> vcsDirectoryMappings = new ArrayList<VcsDirectoryMapping>(myVcsManager.getDirectoryMappings());
    final FixedRoots fixedRoots = calculateNewRoots(vcsDirectoryMappings);
    final HashSet<String> mapped = fixedRoots.mapped;
    final HashSet<String> removed = fixedRoots.removed;
    final HashSet<String> added = fixedRoots.added;

    if (added.isEmpty() && removed.isEmpty()) {
      Messages.showInfoMessage(myProject,
          BzrVcsMessages.message("fix.roots.valid.message"), BzrVcsMessages.message("fix.roots.valid.title"));
      return true;
    }
    BzrFixRootsDialog d = new BzrFixRootsDialog(myProject, mapped, added, removed);
    d.show();
    if (!d.isOK()) {
      return false;
    }
    for (Iterator<VcsDirectoryMapping> i = vcsDirectoryMappings.iterator(); i.hasNext();) {
      VcsDirectoryMapping m = i.next();
      String path = m.getDirectory();
      if (removed.contains(path) || (path.length() == 0 && removed.contains(baseDir.getPath()))) {
        i.remove();
      }
    }
    for (String a : added) {
      vcsDirectoryMappings.add(new VcsDirectoryMapping(a, myVcs.getName()));
    }
    myVcsManager.setDirectoryMappings(vcsDirectoryMappings);
    myVcsManager.updateActiveVcss();
    return true;
  }

  /**
   * Look up file in the file system
   *
   * @param path the path to lookup
   * @return the file or null if the file not found
   */
  @Nullable
  private VirtualFile lookupFile(String path) {
    return myLocalFileSystem.findFileByPath(path);
  }

  /**
   * Look up file in the file system
   *
   * @param path the path to lookup
   * @return the file or null if the file not found
   */
  @Nullable
  private VirtualFile lookupFileRoot(String path) {
    File rootedPath = BzrCoreUtil.getBzrRoot(new File(path));
    if (rootedPath == null)
      return null;
    return myLocalFileSystem.findFileByPath(rootedPath.getAbsolutePath());
  }

  /**
   * Add subroots for the content root
   *
   * @param directory the content root to check
   * @param toAdd     collection of roots to be added
   * @param mapped    all mapped git roots
   */
  private static void addSubroots(VirtualFile directory, HashSet<String> toAdd, HashSet<String> mapped) {
    for (VirtualFile child : directory.getChildren()) {
      if (!child.isDirectory()) {
        continue;
      }
      if (child.getName().equals(".bzr") && child.findChild("branch") != null && !mapped.contains(directory.getPath())) {
        toAdd.add(directory.getPath());
      } else {
        addSubroots(child, toAdd, mapped);
      }
    }
  }

  /**
   * Invalidate git root
   */
  private void invalidate() {
    myRootsInvalidated.set(true);
  }

  private class FixedRoots {
    public final HashSet<String> mapped = new HashSet<String>();
    public final HashSet<String> removed = new HashSet<String>();
    public final HashSet<String> added = new HashSet<String>();
    public final int m_rootsHashCode;

    public FixedRoots(int rootsHashCode) {
      m_rootsHashCode = rootsHashCode;
    }
  }

  /**
   * The listener for git roots
   */
  private class MyFileListener extends VirtualFileAdapter {
    /**
     * Return true if file has bzr repositories
     *
     * @param file the file to check
     * @return true if file has bzr repositories
     */
    private boolean hasBzrRepositories(VirtualFile file) {
      if (!file.isDirectory() || !(file.getName().equals(".bzr") && file.findChild("branch") != null)) {
        return false;
      }
      VirtualFile baseDir = myProject.getBaseDir();
      if (baseDir == null) {
        return false;
      }
      if (!VfsUtil.isAncestor(baseDir, file, false)) {
        boolean isUnder = false;
        for (VirtualFile c : myProjectRoots.getContentRoots()) {
          if (!VfsUtil.isAncestor(baseDir, c, false) && VfsUtil.isAncestor(c, file, false)) {
            isUnder = true;
            break;
          }
        }
        if (!isUnder) {
          return false;
        }
      }
      return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void fileCreated(VirtualFileEvent event) {
      if (!myHasBzrRoots.get()) {
        return;
      }
      if (hasBzrRepositories(event.getFile())) {
        invalidate();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeFileDeletion(VirtualFileEvent event) {
      if (!myHasBzrRoots.get()) {
        return;
      }
      if (hasBzrRepositories(event.getFile())) {
        invalidate();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fileMoved(VirtualFileMoveEvent event) {
      if (!myHasBzrRoots.get()) {
        return;
      }
      if (hasBzrRepositories(event.getFile())) {
        invalidate();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fileCopied(VirtualFileCopyEvent event) {
      if (!myHasBzrRoots.get()) {
        return;
      }
      if (hasBzrRepositories(event.getFile())) {
        invalidate();
      }
    }
  }
}
