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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.patch.formove.FilePathComparator;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeProvider;
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
import bazaar4idea.provider.commit.BzrCommitExecutor;
import bazaar4idea.provider.update.BzrIntegrateEnvironment;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Patrick Woodworth
 */
public class BzrVcs extends AbstractVcs<CommittedChangeList> implements Disposable {

  static final Logger LOG = Logger.getInstance(BzrVcs.class.getName());

  public static final String VCS_NAME = "Bazaar";

  public static final String VCS_METADATA_DIR = ".bzr";

  private final static VcsKey ourKey = createKey(VCS_NAME);

  public static final Topic<BzrUpdater> BRANCH_TOPIC = new Topic<BzrUpdater>("bzr4intellij.branch", BzrUpdater.class);
  public static final Topic<BzrUpdater> INCOMING_TOPIC =
      new Topic<BzrUpdater>("bzr4intellij.incoming", BzrUpdater.class);
  public static final Topic<BzrUpdater> OUTGOING_TOPIC =
      new Topic<BzrUpdater>("bzr4intellij.outgoing", BzrUpdater.class);

  public static final Icon BAZAAR_ICON = IconLoader.getIcon("/org/emergent/bzr4j/intellij/bzr.png");
  public static final Icon INCOMING_ICON = IconLoader.getIcon("/actions/moveDown.png");
  public static final Icon OUTGOING_ICON = IconLoader.getIcon("/actions/moveUp.png");

  private static final String ourRevisionPattern = "\\d+(\\.\\d+)*";

  private final AnnotationProvider m_annotationProvider;

  private final DiffProvider m_diffProvider;

  private final CheckinEnvironment m_checkinEnvironment;

  private final ChangeProvider m_changeProvider;

  private final VcsHistoryProvider m_vcsHistoryProvider;

  private final RollbackEnvironment m_rollbackEnvironment;

  private final MergeProvider m_mergeProvider;

  private final UpdateEnvironment updateEnvironment;
  private final BzrIntegrateEnvironment integrateEnvironment;

  private final BzrProjectConfigurable m_configurable;

  private Disposable m_activationDisposable;

  private BzrVirtualFileListener virtualFileListener;

  private final BzrCommitExecutor commitExecutor;
  private final BzrCurrentBranchStatus hgCurrentBranchStatus = new BzrCurrentBranchStatus();
  private final BzrChangesetStatus incomingChangesStatus = new BzrChangesetStatus(BzrVcs.INCOMING_ICON);
  private final BzrChangesetStatus outgoingChangesStatus = new BzrChangesetStatus(BzrVcs.OUTGOING_ICON);
  private MessageBusConnection messageBusConnection;
  private ScheduledFuture<?> changesUpdaterScheduledFuture;

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
    return (BzrVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(VCS_NAME);
  }

  public BzrVcs(@NotNull final Project project) {
    super(project, VCS_NAME);

    m_configurable = new BzrProjectConfigurable(project);
    m_changeProvider = new BzrChangeProvider(project, getKeyInstanceMethod());
    virtualFileListener = new BzrVirtualFileListener(project, this);
    m_rollbackEnvironment = new BzrRollbackEnvironment(project);
    m_diffProvider = new BzrDiffProvider(project);
    m_vcsHistoryProvider = new BzrHistoryProvider(project);
    m_checkinEnvironment = new BzrCheckinEnvironment(project);
    m_annotationProvider = new BzrAnnotationProvider(project);
    commitExecutor = new BzrCommitExecutor(project);

    m_mergeProvider = null;
    updateEnvironment = new BzrUpdateEnvironment(project);
    integrateEnvironment = BzrDebug.EXPERIMENTAL_ENABLED ? new BzrIntegrateEnvironment(project) : null;

//        LogUtil.dumpImportantData(new Properties());
  }

  @NonNls
  public String getDisplayName() {
    return m_configurable.getDisplayName();
  }

  public void dispose() {
  }

  public Configurable getConfigurable() {
    return m_configurable;
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
    return m_diffProvider;
  }

  public AnnotationProvider getAnnotationProvider() {
    if (!started) {
      return null;
    }
    return m_annotationProvider;
  }

  public VcsHistoryProvider getVcsHistoryProvider() {
    if (!started) {
      return null;
    }
    return m_vcsHistoryProvider;
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
    return m_changeProvider;
  }

  public MergeProvider getMergeProvider() {
    if (!started) {
      return null;
    }
    return m_mergeProvider;
  }

  public RollbackEnvironment getRollbackEnvironment() {
    if (!started) {
      return null;
    }
    return m_rollbackEnvironment;
  }

  public CheckinEnvironment getCheckinEnvironment() {
    if (!started) {
      return null;
    }
    return m_checkinEnvironment;
  }

  @Override
  public UpdateEnvironment getUpdateEnvironment() {
    if (!started) {
      return null;
    }

    return updateEnvironment;
  }

  @Override
  public UpdateEnvironment getIntegrateEnvironment() {
    if (!started) {
      return null;
    }

    return integrateEnvironment;
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
    lfs.addVirtualFileListener(virtualFileListener);
    lfs.registerAuxiliaryFileOperationsHandler(virtualFileListener);
    CommandProcessor.getInstance().addCommandListener(virtualFileListener);

    BzrGlobalSettings globalSettings = BzrGlobalSettings.getInstance();
    BzrProjectSettings projectSettings = BzrProjectSettings.getInstance(myProject);

    ChangeListManager.getInstance(myProject).registerCommitExecutor(commitExecutor);

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
    lfs.removeVirtualFileListener(virtualFileListener);
    lfs.unregisterAuxiliaryFileOperationsHandler(virtualFileListener);
    CommandProcessor.getInstance().removeCommandListener(virtualFileListener);

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
            if (!fileTypeMgr.isFileIgnored(VCS_METADATA_DIR)) {
              String ignoredList = fileTypeMgr.getIgnoredFilesList();
              StringBuffer newList = new StringBuffer(ignoredList);
              if (!ignoredList.endsWith(";"))
                newList.append(';');
              newList.append(VCS_METADATA_DIR);
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
}
