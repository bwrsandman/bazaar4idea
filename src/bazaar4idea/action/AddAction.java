package bazaar4idea.action;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import static com.intellij.openapi.vcs.VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
import static com.intellij.openapi.vcs.VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import bazaar4idea.BzrFile;
import bazaar4idea.BzrVcs;
import bazaar4idea.BzrVcsMessages;
import bazaar4idea.command.BzrAddCommand;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Action for adding new files under VCS
 *
 * @author Eugeny Schava
 */
public class AddAction extends BzrAbstractFilesAction {

  private static final Logger LOG = Logger.getInstance(AddAction.class.getName());

  protected boolean isEnabled(Project project, BzrVcs vcs, VirtualFile file) {
    if (file.isDirectory()) return true;
    final FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
//        LOG.debug("AddAction status: " + String.valueOf( fileStatus ));
    return fileStatus != null && FileStatus.UNKNOWN.equals(fileStatus);
  }

  @Override
  protected void batchPerform(Project project, BzrVcs activeVcs, List<VirtualFile> files, DataContext context)
      throws VcsException {
//        Collection<VcsException> cex = new LinkedList<VcsException>();
    for (VirtualFile vFile : files) {
//            try
//            {
      doPerform(project, activeVcs, vFile, context);
//            }
//            catch ( VcsException e )
//            {
//                cex.add( e );
//            }
    }
//        if ( !cex.isEmpty() )
//            throw new CompositeVcsException( cex );
  }

  private void doPerform(Project project, BzrVcs activeVcs, VirtualFile file, DataContext context)
      throws VcsException {
    try {
      addFileUnderVcsConfirm(project, activeVcs, file);
    }
    catch (IOException e) {
      VcsException ve = new VcsException(e);
      ve.setVirtualFile(file);
      throw ve;
    }
  }

  private static void addFileUnderVcsConfirm(Project project, BzrVcs vcs, VirtualFile file) throws IOException {
    if (!VcsUtil.isFileForVcs(file, project, vcs)) {
      return;
    }
    if (!isFileProcessable(file)) {
      return;
    }

    Object[] params1 = new Object[] { };
    String title = BzrVcsMessages.message("bzr4intellij.add.confirmation.title", params1);
    Object[] params = new Object[] { file.getPath() };
    String message = BzrVcsMessages.message("bzr4intellij.add.confirmation.body", params);

    VcsShowConfirmationOption option = ProjectLevelVcsManager.getInstance(project)
        .getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, vcs);

    boolean processAdd = false;
    if (DO_ACTION_SILENTLY == option.getValue()) {
      processAdd = true;
    } else if (SHOW_CONFIRMATION == option.getValue()) {
      AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
      processAdd = null != helper.selectFilesToProcess(
          Arrays.asList(file), title, null, title, message, option
      );
    }
    VirtualFile repo = VcsUtil.getVcsRootFor(project, file);
    if (processAdd && repo != null) {
      new BzrAddCommand(project).execute(new BzrFile(repo, VfsUtil.virtualToIoFile(file)));
    }
  }

  protected static boolean isFileProcessable(VirtualFile file) {
    if (file == null) {
      return false;
    }
//      ChangeListManager changeListManager = ChangeListManager.getInstance(project);
//      return !FileTypeManager.getInstance().isFileIgnored(file.getName())
//        || !changeListManager.isIgnoredFile(file);
    return true;
  }
}
