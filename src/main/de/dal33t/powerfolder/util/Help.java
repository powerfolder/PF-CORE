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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkJButton;
import de.dal33t.powerfolder.ui.widget.LinkLabel;

/**
 * A general class to open help topics.
 * <p>
 * The general documentation strategy is to use online help in webbrowser.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class Help {
    private static final Logger LOG = Logger.getLogger(Help.class.getName());

    /**
     * Static class, no instance allowed
     */
    private Help() {
    }

    /**
     * Opens the quickstart guides
     */
    public static void openQuickstartGuides(Controller controller) {
        LOG.fine("Opening quickstart guides");
        try {
            BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_QUICKSTART_URL
                .getValue(controller));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to open quickstart guides", e);
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
    public static LinkLabel createQuickstartGuideLabel(Controller controller,
        String labelText)
    {
        LinkLabel label = new LinkLabel(labelText,
            ConfigurationEntry.PROVIDER_QUICKSTART_URL.getValue(controller));
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
    public static LinkJButton createWikiLinkButton(Controller controller,
        String article)
    {
        String toolTips = Translation.getTranslation("general.what_is_this");
        return new LinkJButton(Icons.QUESTION, toolTips,
            ConfigurationEntry.PROVIDER_WIKI_URL.getValue(controller) + "/"
                + article);
    }
}
