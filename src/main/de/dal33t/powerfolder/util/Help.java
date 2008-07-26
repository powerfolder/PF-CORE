/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.util;

import java.io.IOException;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.Icons;

/**
 * A general class to open help topics.
 * <p>
 * The general documentation strategy is to use online help in webbrowser.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class Help {

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
        Loggable.logFineStatic(Help.class,
                "Opening help. Homepage nodeId '" + homepageNodeId + "'");
        try {
            BrowserLauncher.openURL(Constants.POWERFOLDER_URL + "/"
                + homepageNodeId);
        } catch (IOException e) {
            Loggable.logSevereStatic(Help.class,
                    "Unable to open help. Homepage nodeId '" + homepageNodeId
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
        LinkLabel label = new LinkLabel(labelText, Constants.POWERFOLDER_URL
            + "/" + homepageNodeId);
        label.setBorder(Borders.createEmptyBorder("0,1,0,0"));
        return label;
    }

    /**
     * Creates a linklabel, which links to a article on the PowerFolder wiki.
     * 
     * @param labelText
     *            the text of the lable
     * @param article
     *            The article url. e.g. LAN-IP-List for
     *            http://wiki.powerfolder.com/wiki/LAN-IP-List
     * @return a lable that is clickable
     */
    public static LinkLabel createWikiLinkLabel(String article)
    {
        String toolTips = Translation
                .getTranslation("general.what_is_this");
        return new LinkLabel(Icons.QUESTION,
                toolTips,
                Constants.POWERFOLDER_WIKI_URL
            + "/" + article);
    }
}
