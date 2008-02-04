package de.dal33t.powerfolder.util;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * A Thread that can be manually invoked to check for updates to PowerFolder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a> *
 * @version $Revision: 1.3 $
 */
public class ManuallyInvokedUpdateChecker extends UpdateChecker {

    public ManuallyInvokedUpdateChecker(Controller controller,
        UpdateSetting settings)
    {
        super(controller, settings);
    }

    /**
     * Notifies user that no update is available
     */
    @Override
    protected void notifyNoUpdateAvailable() {
        if (controller.isUIEnabled()) {
            DialogFactory.genericDialog(
                    getParentFrame(),
                    Translation.getTranslation("dialog.updatecheck.noUpdateAvailable"),
                    Translation.getTranslation("dialog.updatecheck.noUpdateAvailable"),
                    GenericDialogType.INFO);
        }
    }

    protected boolean shouldCheckForNewerVersion() {
        return true;
    }
}