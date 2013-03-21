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
package de.dal33t.powerfolder.ui.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.Icons;
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
        if (StringUtils.isBlank(ConfigurationEntry.PROVIDER_QUICKSTART_URL
            .getValue(controller)))
        {
            return;
        }
        LOG.fine("Opening quickstart guides");
        try {
            BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_QUICKSTART_URL
                .getValue(controller));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to open quickstart guides", e);
        }
    }

    /**
     * Opens the quickstart guides
     * 
     * @param controller
     * @param article
     */
    public static void openWikiArticle(Controller controller, String article) {
        LOG.fine("Opening wiki article '" + article + '\'');
        try {
            BrowserLauncher.openURL(getWikiArticleURL(controller, article));
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
     * @return a lable that is clickable
     */
    public static LinkLabel createQuickstartGuideLabel(Controller controller,
        String labelText)
    {
        String url = ConfigurationEntry.PROVIDER_QUICKSTART_URL
            .getValue(controller);
        if (StringUtils.isNotBlank(url)) {
            return new LinkLabel(controller, labelText,
                ConfigurationEntry.PROVIDER_QUICKSTART_URL.getValue(controller));
        } else {
            return new LinkLabel(controller, "", "");
        }

    }

    /**
     * @param controller
     * @return true if a wiki is available
     */
    public static boolean hasWiki(Controller controller) {
        String wikiURL = ConfigurationEntry.PROVIDER_WIKI_URL
            .getValue(controller);
        return StringUtils.isNotBlank(wikiURL);
    }

    /**
     * @param controller
     *            the controller.
     * @param article
     *            the article name, e.g. "Supernode"
     * @return the URL of the article e.g.
     *         http://www.powerfolder.com/wiki/Supernode
     */
    public static String getWikiArticleURL(Controller controller, String article)
    {
        String wikiURL = ConfigurationEntry.PROVIDER_WIKI_URL
            .getValue(controller);
        if (StringUtils.isBlank(wikiURL)) {
            LOG.log(Level.SEVERE, "Unable to find wiki URL for article " + article);
            return null;
        }
        return wikiURL + '/' + article;
    }

    /**
     * Creates a linklabel, which links to a article on the PowerFolder wiki.
     * 
     * @param article
     *            The article url. e.g. LAN-IP-List for
     *            http://wiki.powerfolder.com/wiki/LAN-IP-List
     * @return a lable that is clickable
     */
    public static LinkJButton createWikiLinkButton(Controller controller,
        String article)
    {
        String toolTips = Translation.getTranslation("general.what_is_this");
        String link = getWikiArticleURL(controller, article);
        LinkJButton b = new LinkJButton(Icons.getIconById(Icons.QUESTION),
            toolTips, link);
        b.setVisible(StringUtils.isNotBlank(link));
        return b;
    }
}
