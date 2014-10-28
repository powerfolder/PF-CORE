package de.dal33t.powerfolder.ui.contextmenu;

import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

class VersionHistoryAction extends PFContextMenuAction {

    VersionHistoryAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        final List<FileInfo> fileInfos = getFileInfos(paths);

        UIUtil.invokeLaterInEDT(new Runnable() {
            @Override
            public void run() {
                PFWizard.openMultiFileRestoreWizard(getController(), fileInfos);
            }
            
        });
    }

}
