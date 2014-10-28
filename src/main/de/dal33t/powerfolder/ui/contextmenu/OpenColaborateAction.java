package de.dal33t.powerfolder.ui.contextmenu;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.util.UIUtil;

class OpenColaborateAction extends PFContextMenuAction {

    private static final Logger log = Logger
        .getLogger(OpenColaborateAction.class.getName());

    OpenColaborateAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        if (!Desktop.isDesktopSupported()) {
            log.info("Won't be able to open file. Unsupported operation required.");
            return;
        }

        List<FileInfo> files = getFileInfos(paths);

        for (final FileInfo file : files) {
            if (file.isDiretory()
                || SyncStatus.of(getController(), file) == SyncStatus.IGNORED)
            {
                continue;
            }

            UIUtil.invokeLaterInEDT(new Runnable() {
                @Override
                public void run() {
                    try {
                        Path path = file.getDiskFile(getController()
                            .getFolderRepository());
                        Desktop.getDesktop().open(path.toFile());
                        file.lock(getController());
                    } catch (IOException ioe) {
                        log.warning("Could not open file " + file
                            + " for editing. " + ioe);
                    }
                }
            });
        }
    }

}
