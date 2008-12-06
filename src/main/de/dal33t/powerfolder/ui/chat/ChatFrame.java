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
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.plaf.RootPaneUI;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * The information window.
 */
public class ChatFrame extends PFUIComponent {

    private JFrame uiComponent;
    private JTabbedPane tabbedPane;
    private final Map<MemberInfo, ChatPanel> memberPanels;

    /**
     * Constructor.
     *
     * @param controller
     */
    public ChatFrame(Controller controller) {
        super(controller);
        tabbedPane = new JTabbedPane();
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        memberPanels = new HashMap<MemberInfo, ChatPanel>();
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
        Member member = getController().getNodeManager().getNode(memberInfo);
        ChatPanel chatPanel = new ChatPanel(getController(), this, member);
        memberPanels.put(memberInfo, chatPanel);
        tabbedPane.addTab(memberInfo.nick, Icons.getIconFor(member),
                chatPanel.getUiComponent(), Translation.getTranslation(
                "chat_frame.tool_tip", member.getNick()));
        tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
    }

    /**
     * Update the icons on the tabs as members come and go.
     *
     * @param member
     */
    private void updateTabIcons(Member member) {
        for (MemberInfo memberInfo : memberPanels.keySet()) {
            if (member.getInfo().equals(memberInfo)) {
                Icon icon = Icons.getIconFor(member);
                Component component = memberPanels.get(memberInfo).getUiComponent();
                int count = tabbedPane.getComponentCount();
                for (int i = 0; i < count; i++) {
                    if (tabbedPane.getComponentAt(i).equals(component)) {
                        tabbedPane.setIconAt(i, icon);
                    }
                }
            }
        }
    }

    /**
     * Close a chat session tab.
     * 
     * @param chatPartner
     */
    public void closeSession(Member chatPartner) {
        for (Iterator<MemberInfo> iter = memberPanels.keySet().iterator(); iter.hasNext();) {
            MemberInfo memberInfo = iter.next();
            if (memberInfo.equals(chatPartner.getInfo())) {
                Component component = memberPanels.get(memberInfo).getUiComponent();
                int count = tabbedPane.getComponentCount();
                for (int i = 0; i < count; i++) {
                    if (tabbedPane.getComponentAt(i).equals(component)) {
                        tabbedPane.remove(i);
                        iter.remove();
                        break;
                    }
                }
            }
        }

        // No tabs? - so hide
        if (tabbedPane.getComponentCount() == 0) {
            getUIComponent().setVisible(false);
        }
    }

    ///////////////////
    // INNER CLASSES //
    ///////////////////

    /**
     * Listens on changes in the online state and update the ui components
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public void nodeRemoved(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void friendAdded(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}