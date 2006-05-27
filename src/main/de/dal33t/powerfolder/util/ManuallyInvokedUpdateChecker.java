package de.dal33t.powerfolder.util;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;

/**
 * A Thread that can be manually invoked to check for updates to
 * PowerFolder
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a> * 
 * @version $Revision: 1.3 $
 */
public class ManuallyInvokedUpdateChecker extends UpdateChecker {

    public ManuallyInvokedUpdateChecker(Controller controller) {
        super(controller);
    }

    /**
     * Notifies user that no update is available
     */
    @Override
    protected void notifyNoUpdateAvailable()
    {
        if (newerDevelopmentVersionAvailable() == null
            && controller.isUIEnabled())
        {
            JOptionPane.showMessageDialog(getParentFrame(), Translation
                .getTranslation("dialog.updatecheck.noUpdateAvailable"));
        }
    }

    /**
     * Notifies user that no development update is available
     */
    @Override
    protected void notifyNoDevelopmentUpdateAvailable()
    {
        if (shouldCheckForNewerVersion()
            && newerReleaseVersionAvailable() == null
            && controller.isUIEnabled())
        {
            JOptionPane
                .showMessageDialog(
                    getParentFrame(),
                    Translation
                        .getTranslation("dialog.updatecheck.developmentversion.noUpdateAvailable"));
        }
    }
    
    protected boolean shouldCheckForNewerVersion() {
        return true;
    }
}