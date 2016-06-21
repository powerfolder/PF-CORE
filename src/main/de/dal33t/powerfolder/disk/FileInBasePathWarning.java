package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.util.PathUtils;

public final class FileInBasePathWarning extends WarningNotice {
    private static final long serialVersionUID = 1L;

    FileInBasePathWarning(String title, String summary, String message) {
        super(title, summary, message);
    }

    @Override
    public Runnable getPayload(final Controller controller) {
        return new Runnable() {
            public void run() {
                PathUtils.openFile(controller.getFolderRepository()
                    .getFoldersBasedir());
                DialogFactory.genericDialog(controller, getTitle(),
                    getMessage(), GenericDialogType.WARN);
            }
        };
    }
}