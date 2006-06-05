/* $Id: Help.java,v 1.2 2005/11/05 01:07:34 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.util.ui.LinkLabel;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectionBox;

/**
 * A general class to open help topics.
 * <p>
 * The general documentation strategy is to use online help in webbrowser.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class Help {
    private static final Logger LOG = Logger.getLogger(Help.class);

    /**
     * Static class, no instance allowed
     */
    private Help() {
    }

    /**
     * Opens a help topic from the PowerFolder homepage
     * 
     * @param homepageNodeId
     *            the node id on the powerfolder homepage of this topic. e.g
     *            node/faq
     */
    public static void openHelp(String homepageNodeId) {
        // TODO: Might show a message box to the user, that the help topic will
        // be opend in browser
        LOG.debug("Opening help. Homepage nodeId '" + homepageNodeId + "'");
        try {
            BrowserLauncher.openURL(Constants.POWERFOLDER_URL + "/?q="
                + homepageNodeId);
        } catch (IOException e) {
            LOG.error("Unable to open help. Homepage nodeId '" + homepageNodeId
                + "'", e);
            // TODO: Show a message box to the user
        }
    }

    /**
     * Creates a linklabel, which links to a help topic on the PowerFolder
     * homepage.
     * 
     * @param labelText
     *            the text of the lable
     * @param homepageNodeId
     *            the node id on the powerfolder homepage of this topic. e.g
     *            node/faq
     * @return a lable that is clickable
     */
    public static LinkLabel createHelpLinkLabel(String labelText,
        String homepageNodeId)
    {
        return new LinkLabel(labelText, Constants.POWERFOLDER_URL + "/?q="
            + homepageNodeId);
    }

    /**
     * Adds a help label to the sync selection box. It is placed on the right
     * side of the drop down list. TODO: Add better column definitions
     * 
     * @param syncSelectBox
     * @return
     */
    public static final JComponent addHelpLabel(
        SyncProfileSelectionBox syncSelectBox)
    {
        if (syncSelectBox == null) {
            throw new NullPointerException("Sync selection box is null");
        }
        FormLayout layout = new FormLayout("fill:pref:grow, 3dlu, pref, 6dlu",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        JLabel helpLabel = Help.createHelpLinkLabel("help", "node/syncoptions");

        builder.add(syncSelectBox, cc.xy(1, 1));
        builder.add(helpLabel, cc.xy(3, 1));
        return builder.getPanel();
    }
}
