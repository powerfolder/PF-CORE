package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class DeviceDisconnectedProblem extends ResolvableProblem {
    private final FolderInfo folderInfo;

    public DeviceDisconnectedProblem(FolderInfo folderInfo) {
        Reject.ifNull(folderInfo, "Folder");
        this.folderInfo = folderInfo;
    }

    @Override
    public String getDescription() {
        return Translation.getTranslation("folder_problem.device_disconnected");
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_DEVICE_DISCONNECTED;
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
            .getTranslation("folder_problem.device_disconnected.remove_folder");
    }

}
