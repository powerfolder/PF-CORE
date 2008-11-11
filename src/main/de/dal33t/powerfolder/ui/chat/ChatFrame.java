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
* $Id: ChatFrame.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.chat;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.InformationFilesCard;
import de.dal33t.powerfolder.ui.information.InformationSettingsCard;
import de.dal33t.powerfolder.ui.information.InformationMembersCard;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.*;
import javax.swing.plaf.RootPaneUI;
import java.util.prefs.Preferences;
import java.awt.*;

/**
 * The information window.
 */
public class ChatFrame extends PFUIComponent {

    private JFrame uiComponent;

    /**
     * Constructor
     *
     * @param controller
     */
    public ChatFrame(Controller controller) {
        super(controller);
    }

    /**
     * Returns the ui component.
     *
     * @return
     */
    public JFrame getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Builds the UI component.
     */
    private void buildUIComponent() {
        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("chatframe4.x", 50), prefs.getInt(
            "chatframe4.y", 50));

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("chatframe4.width", 500);
        int height = prefs.getInt("chatframe4.height", 600);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        uiComponent.setSize(width, height);

        if (prefs.getBoolean("chatframe4.maximized", false)) {
            // Fix Synthetica maximization, otherwise it covers the task bar.
            // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
            RootPaneUI ui = uiComponent.getRootPane().getUI();
            if (ui instanceof SyntheticaRootPaneUI) {
                ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
            }
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // everything is decided in window listener
        uiComponent.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    /**
     * Initializes the components.
     */
    private void initialize() {
        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.CHAT_IMAGE);
        uiComponent.setTitle(Translation.getTranslation("chat_frame.title"));
    }

    /**
     * Stores all current window valus.
     */
    public void storeValues() {
        Preferences prefs = getController().getPreferences();
        if (uiComponent == null) {
            return;
        }
        if ((uiComponent.getExtendedState() &
                Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            prefs.putBoolean("chatframe4.maximized", true);
        } else {
            prefs.putInt("chatframe4.x", uiComponent.getX());
            if (uiComponent.getWidth() > 0) {
                prefs.putInt("chatframe4.width", uiComponent.getWidth());
            }
            prefs.putInt("chatframe4.y", uiComponent.getY());
            if (uiComponent.getHeight() > 0) {
                prefs.putInt("chatframe4.height", uiComponent.getHeight());
            }
            prefs.putBoolean("chatframe4.maximized", false);
        }
    }

    public void displayChat(MemberInfo memberInfo) {

    }
}