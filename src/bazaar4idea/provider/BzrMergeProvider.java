package bazaar4idea.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.merge.MergeData;
import com.intellij.openapi.vcs.merge.MergeProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import bazaar4idea.command.BzrResolveCommand;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class BzrMergeProvider implements MergeProvider {

  private final Project m_project;

  public BzrMergeProvider(final Project project) {
    m_project = project;
  }

  @NotNull
  public MergeData loadRevisions(final VirtualFile file) throws VcsException {
    final MergeData data = new MergeData();
    VcsRunnable runnable = new VcsRunnable() {
      public void run() throws VcsException {
        File oldFile = new File(file.getPath() + ".BASE");
        File newFile = new File(file.getPath() + ".OTHER");
        File workingFile = new File(file.getPath() + ".THIS");
        data.ORIGINAL = readFile(oldFile);
        data.LAST = readFile(newFile);
        data.CURRENT = readFile(workingFile);
      }
    };
    VcsUtil.runVcsProcessWithProgress(runnable,
        VcsBundle.message("multiple.file.merge.loading.progress.title"), false, m_project);

    return data;
  }

  public void conflictResolvedForFile(VirtualFile file) {
    BzrResolveCommand resolveCommand = new BzrResolveCommand(m_project);
    VirtualFile root = VcsUtil.getVcsRootFor(m_project, file);
    if (root == null) {
      return;
    }
    resolveCommand.resolve(root, file);
    final VirtualFile parent = file.getParent();
    if (parent != null) {
      parent.refresh(true, false);
    }
  }

  public boolean isBinary(final VirtualFile file) {
    try {
      byte[] bytes = readFile(new File(file.getPath()));
      for (byte aByte : bytes) {
        if (aByte == 0)
          return true;
      }
    } catch (VcsException ignored) {
    }
    return false;
  }

  private static byte[] readFile(File workingFile) throws VcsException {
    try {
      return FileUtil.loadFileBytes(workingFile);
    }
    catch (IOException e) {
      throw new VcsException(e);
    }
  }
}
