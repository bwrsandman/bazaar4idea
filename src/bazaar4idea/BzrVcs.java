/*
 * Copyright (c) 2010 Patrick Woodworth
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

import bazaar4idea.command.Bzr;
import bazaar4idea.config.BzrExecutableValidator;
import bazaar4idea.config.BzrVcsApplicationSettings;
import bazaar4idea.config.BzrVcsSettings;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.patch.formove.FilePathComparator;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.merge.MergeProvider;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ComparatorDelegate;
import com.intellij.util.containers.Convertor;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.UIUtil;
import bazaar4idea.provider.BzrChangeProvider;
import bazaar4idea.provider.BzrDiffProvider;
import bazaar4idea.provider.BzrHistoryProvider;
import bazaar4idea.provider.BzrRollbackEnvironment;
import bazaar4idea.provider.annotate.BzrAnnotationProvider;
import bazaar4idea.provider.commit.BzrCheckinEnvironment;
import bazaar4idea.provider.update.BzrUpdateEnvironment;
import bazaar4idea.ui.BzrChangesetStatus;
import bazaar4idea.ui.BzrCurrentBranchStatus;
import org.emergent.bzr4j.core.utils.BzrCoreUtil;
import bazaar4idea.ui.BzrRootTracker;
import bazaar4idea.util.BzrDebug;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Patrick Woodworth
 */
public class BzrVcs extends AbstractVcs<CommittedChangeList> implements Disposable {
  public static final NotificationGroup NOTIFICATION_GROUP_ID = NotificationGroup.toolWindowGroup(
          "Bazaar Messages", ChangesViewContentManager.TOOLWINDOW_ID, true);
  public static final NotificationGroup IMPORTANT_ERROR_NOTIFICATION = new NotificationGroup(
          "Bazaar Important Messages", NotificationDisplayType.STICKY_BALLOON, true);

  static final Logger LOG = Logger.getInstance(BzrVcs.class.getName());

  public static final String NAME = "Bazaar";

  private final static VcsKey ourKey = createKey(NAME);

  public static final Topic<BzrUpdater> BRANCH_TOPIC = new Topic<BzrUpdater>("bzr4intellij.branch", BzrUpdater.class);
  public static final Topic<BzrUpdater> INCOMING_TOPIC =
      new Topic<BzrUpdater>("bzr4intellij.incoming", BzrUpdater.class);
  public static final Topic<BzrUpdater> OUTGOING_TOPIC =
      new Topic<BzrUpdater>("bzr4intellij.outgoing", BzrUpdater.class);

  public static final Icon BAZAAR_ICON = IconLoader.getIcon("/org/emergent/bzr4j/intellij/bzr.png");
  public static final Icon INCOMING_ICON = IconLoader.getIcon("/actions/moveDown.png");
  public static final Icon OUTGOING_ICON = IconLoader.getIcon("/actions/moveUp.png");

  private static final int MAX_CONSOLE_OUTPUT_SIZE = 10000;

  private static final String ourRevisionPattern = "\\d+(\\.\\d+)*";
  private final ReadWriteLock myCommandLock = new ReentrantReadWriteLock(true); // The command read/write lock
  private final AnnotationProvider myAnnotationProvider;

  private final DiffProvider myDiffProvider;

  private final CheckinEnvironment myCheckinEnvironment;

  private final ChangeProvider myChangeProvider;

  private final VcsHistoryProvider myHistoryProvider;
  @NotNull private final Bzr myBzr;
  private final RollbackEnvironment myRollbackEnvironment;

  private final UpdateEnvironment myUpdateEnvironment;
//  private final BzrIntegrateEnvironment myIntegrateEnvironment;

//  private final BzrProjectConfigurable myConfigurable;

  private Disposable m_activationDisposable;

  private final ProjectLevelVcsManager myVcsManager;

  private BzrVirtualFileListener myVirtualFileListener;
  private final BzrExecutableValidator myExecutableValidator;
//  private final BzrCommitExecutor myCommitExecutor;
  private final BzrCurrentBranchStatus hgCurrentBranchStatus = new BzrCurrentBranchStatus();
  private final BzrChangesetStatus incomingChangesStatus = new BzrChangesetStatus(BzrVcs.INCOMING_ICON);
  private final BzrChangesetStatus outgoingChangesStatus = new BzrChangesetStatus(BzrVcs.OUTGOING_ICON);
  private MessageBusConnection messageBusConnection;
  private ScheduledFuture<?> changesUpdaterScheduledFuture;
  private BzrVcsApplicationSettings myAppSettings;

  /**
   * The tracker that checks validity of git roots
   */
  private BzrRootTracker myRootTracker;

  /**
   * The dispatcher object for root events
   */
  private EventDispatcher<BzrRootsListener> myRootListeners = EventDispatcher.create(BzrRootsListener.class);

  private boolean started;

  public static BzrVcs getInstance(@NotNull Project project) {
    return (BzrVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(NAME);
  }

  public BzrVcs(@NotNull final Project project) {
    super(project, NAME);

//    myConfigurable = new BzrProjectConfigurable(project);
    myChangeProvider = new BzrChangeProvider(project, getKeyInstanceMethod());
    myVirtualFileListener = new BzrVirtualFileListener(project, this);
    myRollbackEnvironment = new BzrRollbackEnvironment(project);
    myDiffProvider = new BzrDiffProvider(project);
    myHistoryProvider = new BzrHistoryProvider(project);
    myCheckinEnvironment = new BzrCheckinEnvironment(project);
    myAnnotationProvider = new BzrAnnotationProvider(project);
//    myCommitExecutor = new BzrCommitExecutor(project);

    myUpdateEnvironment = new BzrUpdateEnvironment(project, this, null);
//    myIntegrateEnvironment = BzrDebug.EXPERIMENTAL_ENABLED ? new BzrIntegrateEnvironment(project) : null;
    myExecutableValidator = new BzrExecutableValidator(myProject, this);
    myVcsManager = null;
    myBzr = null;
//        LogUtil.dumpImportantData(new Properties());
  }

  public BzrVcs(@NotNull Project project,
                @NotNull Bzr bzr,
                @NotNull final ProjectLevelVcsManager bzrVcsManager,
                @NotNull final BzrAnnotationProvider bzrAnnotationProvider,
                @NotNull final BzrDiffProvider bzrDiffProvider,
                @NotNull final BzrHistoryProvider bzrHistoryProvider,
                @NotNull final BzrRollbackEnvironment bzrRollbackEnvironment,
                @NotNull final BzrVcsApplicationSettings bzrSettings,
                @NotNull final BzrVcsSettings bzrProjectSettings) {
    super(project, NAME);
    myBzr = bzr;
    myVcsManager = bzrVcsManager;
    myAppSettings = bzrSettings;
    myChangeProvider = project.isDefault() ? null : ServiceManager.getService(project, BzrChangeProvider.class);
    myCheckinEnvironment = project.isDefault() ? null : ServiceManager.getService(project, BzrCheckinEnvironment.class);
    myAnnotationProvider = bzrAnnotationProvider;
    myDiffProvider = bzrDiffProvider;
    myHistoryProvider = bzrHistoryProvider;
    myRollbackEnvironment = bzrRollbackEnvironment;
//    myRevSelector = new BzrRevisionSelector();
//    myConfigurable = new BzrVcsConfigurable(bzrProjectSettings, myProject);
    myUpdateEnvironment = new BzrUpdateEnvironment(myProject, this, bzrProjectSettings);
//    myCommittedChangeListProvider = new BzrCommittedChangeListProvider(myProject);
//    myOutgoingChangesProvider = new BzrOutgoingChangesProvider(myProject);
//    myTreeDiffProvider = new BzrTreeDiffProvider(myProject);
//    myCommitAndPushExecutor = new BzrCommitAndPushExecutor(myCheckinEnvironment);
    myExecutableValidator = new BzrExecutableValidator(myProject, this);
//    myPlatformFacade = ServiceManager.getService(myProject, BzrPlatformFacade.class);
  }

  public ReadWriteLock getCommandLock() {
    return myCommandLock;
  }

  @NonNls
  public String getDisplayName() {
    return NAME;
  }

  public void dispose() {
  }

  public Configurable getConfigurable() {
    return null;
  }

  @Nullable
  public BzrRevisionNumber parseRevisionNumber(String revisionNumberString) {
    BzrRevisionNumber retval = null;
    try {
      retval = BzrRevisionNumber.createBzrRevisionNumber(BzrCoreUtil.parseRevisionNumber(revisionNumberString));
      return retval;
    }
    finally {
      LOG.debug("parseRevisionNumber: " + String.valueOf(retval));
    }
  }

  @Override
  public String getRevisionPattern() {
    return null; // ourRevisionPattern;;
  }

  public DiffProvider getDiffProvider() {
    if (!started) {
      return null;
    }
    return myDiffProvider;
  }

  public AnnotationProvider getAnnotationProvider() {
    if (!started) {
      return null;
    }
    return myAnnotationProvider;
  }

  public VcsHistoryProvider getVcsHistoryProvider() {
    if (!started) {
      return null;
    }
    return myHistoryProvider;
  }

  public VcsHistoryProvider getVcsBlockHistoryProvider() {
    if (!started) {
      return null;
    }
    return getVcsHistoryProvider();
  }

  public ChangeProvider getChangeProvider() {
    if (!started) {
      return null;
    }
    return myChangeProvider;
  }

  public MergeProvider getMergeProvider() {
    if (!started) {
      return null;
    }
    return null;
  }

  public RollbackEnvironment getRollbackEnvironment() {
    if (!started) {
      return null;
    }
    return myRollbackEnvironment;
  }

  public CheckinEnvironment getCheckinEnvironment() {
    if (!started) {
      return null;
    }
    return myCheckinEnvironment;
  }

  @Override
  public UpdateEnvironment getUpdateEnvironment() {
    if (!started) {
      return null;
    }

    return myUpdateEnvironment;
  }

  @Override
  public UpdateEnvironment getIntegrateEnvironment() {
    if (!started) {
      return null;
    }

    return null;
  }

  @Override
  public boolean allowsNestedRoots() {
    if (!BzrDebug.ROOT_REMAPPING_ENABLED)
      return super.allowsNestedRoots();
    return true;
  }

  @Override
  public <S> List<S> filterUniqueRoots(final List<S> in, final Convertor<S, VirtualFile> convertor) {
    if (!BzrDebug.ROOT_REMAPPING_ENABLED)
      return super.filterUniqueRoots(in, convertor);
    LOG.debug("BzrVcs.filterUniqueRoots");
    Collections.sort(in, new ComparatorDelegate<S, VirtualFile>(convertor, FilePathComparator.getInstance()));

    for (int i = 1; i < in.size(); i++) {
      final S sChild = in.get(i);
      final VirtualFile child = convertor.convert(sChild);
      final VirtualFile childRoot = BzrUtil.bzrRootOrNull(child);
      if (childRoot == null) {
        // non-git file actually, skip it
        continue;
      }
      for (int j = i - 1; j >= 0; --j) {
        final S sParent = in.get(j);
        final VirtualFile parent = convertor.convert(sParent);
        // the method check both that parent is an ancestor of the child and that they share common git root
        if (VfsUtil.isAncestor(parent, child, false) && VfsUtil.isAncestor(childRoot, parent, false)) {
          in.remove(i);
          //noinspection AssignmentToForLoopParameter
          --i;
          break;
        }
      }
    }
    return in;
  }

  @Override
  public RootsConvertor getCustomConvertor() {
    if (!BzrDebug.ROOT_REMAPPING_ENABLED)
      return super.getCustomConvertor();
    return BzrRootConverter.INSTANCE;
  }

  @Override
  public boolean isVersionedDirectory(VirtualFile dir) {
    return dir.isDirectory() && BzrUtil.bzrRootOrNull(dir) != null;
  }

  public boolean isStarted() {
    return started;
  }

  /**
   * Add listener for git roots
   *
   * @param listener the listener to add
   */
  public void addGitRootsListener(BzrRootsListener listener) {
    myRootListeners.addListener(listener);
  }

  /**
   * Remove listener for git roots
   *
   * @param listener the listener to remove
   */
  public void removeGitRootsListener(BzrRootsListener listener) {
    myRootListeners.removeListener(listener);
  }

  @Override
  protected void start() throws VcsException {
    started = true;
  }

  @Override
  protected void shutdown() throws VcsException {
    started = false;
  }

  public void activate() {
    if (!started) {
      return;
    }

    LocalFileSystem lfs = LocalFileSystem.getInstance();
    lfs.addVirtualFileListener(myVirtualFileListener);
    lfs.registerAuxiliaryFileOperationsHandler(myVirtualFileListener);
    CommandProcessor.getInstance().addCommandListener(myVirtualFileListener);

    BzrGlobalSettings globalSettings = BzrGlobalSettings.getInstance();
    BzrProjectSettings projectSettings = BzrProjectSettings.getInstance(myProject);

//    ChangeListManager.getInstance(myProject).registerCommitExecutor(myCommitExecutor);

    StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    if (statusBar != null) {
      statusBar.addCustomIndicationComponent(hgCurrentBranchStatus);
      statusBar.addCustomIndicationComponent(incomingChangesStatus);
      statusBar.addCustomIndicationComponent(outgoingChangesStatus);
    }

    final BzrIncomingStatusUpdater incomingUpdater =
        new BzrIncomingStatusUpdater(incomingChangesStatus, projectSettings);

    final BzrOutgoingStatusUpdater outgoingUpdater =
        new BzrOutgoingStatusUpdater(outgoingChangesStatus, projectSettings);

//        changesUpdaterScheduledFuture = JobScheduler.getScheduler().scheduleWithFixedDelay(
//                new Runnable() {
//                    public void run() {
//                        incomingUpdater.update(myProject);
//                        outgoingUpdater.update(myProject);
//                    }
//                }, 0, globalSettings.getIncomingCheckIntervalSeconds(), TimeUnit.SECONDS);

    MessageBus messageBus = myProject.getMessageBus();
    messageBusConnection = messageBus.connect();

    messageBusConnection.subscribe(BzrVcs.INCOMING_TOPIC, incomingUpdater);
    messageBusConnection.subscribe(BzrVcs.OUTGOING_TOPIC, outgoingUpdater);

    messageBusConnection.subscribe(
        BzrVcs.BRANCH_TOPIC, new BzrCurrentBranchStatusUpdater(hgCurrentBranchStatus)
    );

    messageBusConnection.subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerAdapter() {
          @Override
          public void selectionChanged(FileEditorManagerEvent event) {
            Project project = event.getManager().getProject();
            project.getMessageBus()
                .asyncPublisher(BzrVcs.BRANCH_TOPIC)
                .update(project);
          }
        }
    );

    if (BzrDebug.ROOT_REMAPPING_ENABLED && !myProject.isDefault() && myRootTracker == null) {
      myRootTracker = new BzrRootTracker(this, myProject, myRootListeners.getMulticaster());
    }

    final BzrConfigurationValidator configValidator = new BzrConfigurationValidator(myProject);

    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(new Runnable() {
      public void run() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          public void run() {
            fixIgnoreList();
            configValidator.check();
          }
        });
      }
    });

    m_activationDisposable = new Disposable() {
      public void dispose() {
      }
    };
  }

  public void deactivate() {
    if (!started) {
      return;
    }

    if (myRootTracker != null) {
      myRootTracker.dispose();
      myRootTracker = null;
    }

    LocalFileSystem lfs = LocalFileSystem.getInstance();
    lfs.removeVirtualFileListener(myVirtualFileListener);
    lfs.unregisterAuxiliaryFileOperationsHandler(myVirtualFileListener);
    CommandProcessor.getInstance().removeCommandListener(myVirtualFileListener);

    StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    if (messageBusConnection != null) {
      messageBusConnection.disconnect();
    }
    if (changesUpdaterScheduledFuture != null) {
      changesUpdaterScheduledFuture.cancel(true);
    }
    if (statusBar != null) {
      statusBar.removeCustomIndicationComponent(incomingChangesStatus);
      statusBar.removeCustomIndicationComponent(outgoingChangesStatus);
      statusBar.removeCustomIndicationComponent(hgCurrentBranchStatus);
    }

    assert m_activationDisposable != null;
    Disposer.dispose(m_activationDisposable);
    m_activationDisposable = null;
  }

  private static void fixIgnoreList() {
    ApplicationManager.getApplication().runWriteAction(
        new Runnable() {
          public void run() {
            FileTypeManager fileTypeMgr = FileTypeManager.getInstance();
            if (!fileTypeMgr.isFileIgnored(BzrUtil.DOT_BZR)) {
              String ignoredList = fileTypeMgr.getIgnoredFilesList();
              StringBuffer newList = new StringBuffer(ignoredList);
              if (!ignoredList.endsWith(";"))
                newList.append(';');
              newList.append(BzrUtil.DOT_BZR);
              fileTypeMgr.setIgnoredFilesList(newList.toString());
            }
          }
        });
  }

  public BzrRootTracker getMyRootTracker() {
    return myRootTracker;
  }

  public static VcsKey getKey() {
    return ourKey;
  }
  /**
   * Run task in background using the common queue (per project)
   * @param task the task to run
   */
  public static void runInBackground(Task.Backgroundable task) {
    task.queue();
  }

  @NotNull
  public BzrExecutableValidator getExecutableValidator() {
    return myExecutableValidator;
  }

  /**
   * Shows a plain message in the Version Control Console.
   */
  public void showMessages(@NotNull String message) {
    if (message.length() == 0) return;
    showMessage(message, ConsoleViewContentType.NORMAL_OUTPUT.getAttributes());
  }

  /**
   * Shows error message in the Version Control Console
   */
  public void showErrorMessages(final String line) {
    showMessage(line, ConsoleViewContentType.ERROR_OUTPUT.getAttributes());
  }

  /**
   * Show message in the Version Control Console
   * @param message a message to show
   * @param style   a style to use
   */
  private void showMessage(@NotNull String message, final TextAttributes style) {
    if (message.length() > MAX_CONSOLE_OUTPUT_SIZE) {
      message = message.substring(0, MAX_CONSOLE_OUTPUT_SIZE);
    }
    myVcsManager.addMessageToConsoleWindow(message, style);
  }

  /**
   * Shows a command line message in the Version Control Console
   */
  public void showCommandLine(final String cmdLine) {
    SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss.SSS");
    showMessage(f.format(new Date()) + ": " + cmdLine, ConsoleViewContentType.SYSTEM_OUTPUT.getAttributes());
  }

}
