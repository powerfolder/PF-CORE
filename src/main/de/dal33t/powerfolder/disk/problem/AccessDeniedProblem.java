package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class AccessDeniedProblem extends ResolvableProblem {
    private final FolderInfo folderInfo;

    public AccessDeniedProblem(FolderInfo folderInfo) {
        Reject.ifNull(folderInfo, "Folder");
        this.folderInfo = folderInfo;
    }

    @Override
    public String getDescription() {
        return Translation.get("folder_problem.access_denied");
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.SECURITY_PERMISSION;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                final Folder folder = controller.getFolderRepository()
                    .getFolder(folderInfo);
                if (folder != null) {
                    controller.getIOProvider().startIO(new Runnable() {
                        public void run() {
                            controller.getFolderRepository().removeFolder(
                                folder, false);
                        }
                    });
                }
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation
            .get("folder_problem.access_denied.remove_folder");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((folderInfo == null) ? 0 : folderInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AccessDeniedProblem))
            return false;
        AccessDeniedProblem other = (AccessDeniedProblem) obj;
        if (folderInfo == null) {
            if (other.folderInfo != null)
                return false;
        } else if (!folderInfo.equals(other.folderInfo))
            return false;
        return true;
    }
}
