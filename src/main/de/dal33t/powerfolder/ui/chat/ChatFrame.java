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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.plaf.RootPaneUI;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/**
 * The information window.
 */
public class ChatFrame extends PFUIComponent {

    private JFrame uiComponent;
    private JTabbedPane tabbedPane;

    /**
     * Constructor.
     *
     * @param controller
     */
    public ChatFrame(Controller controller) {
        super(controller);
        tabbedPane = new JTabbedPane();
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
    }

    /**
     * Initializes the components.
     */
    private void initialize() {
        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.CHAT_IMAGE);
        uiComponent.setTitle(Translation.getTranslation("chat_frame.title"));
        uiComponent.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                uiComponent.setVisible(false);
            }
        });

        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
                "3dlu, fill:pref:grow, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel panel = builder.getPanel();
        panel.add(tabbedPane, cc.xy(2, 2));

        uiComponent.setContentPane(panel);
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

    /**
     * Display a chat session, creats new one if needed.
     *
     * @param memberInfo
     */
    public void displayChat(MemberInfo memberInfo) {

        // Find existing session.
        for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals(memberInfo.nick)) {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }

        // New session.
        ChatPanel chatPanel = new ChatPanel(getController());
        // @todo hghg make image the friend status icon, with status listener.
        // @todo add tip
        tabbedPane.addTab(memberInfo.nick, Icons.CHAT, chatPanel.getUiComponent());
        tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
    }
}