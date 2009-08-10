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
import de.dal33t.powerfolder.MagneticFrame;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.util.Translation;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.RootPaneUI;
import java.awt.event.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * The information window.
 */
public class ChatFrame extends MagneticFrame {

    private JFrame uiComponent;
    private final JTabbedPane tabbedPane;
    private final Map<MemberInfo, ChatPanel> memberPanels;
    private final List<MemberInfo> newMessages;

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
        tabbedPane.addChangeListener(new MyChangeListener());
        newMessages = new CopyOnWriteArrayList<MemberInfo>();
    }

    public void initializeChatModelListener(ChatModel chatModel) {
        chatModel.addChatModelListener(new MyChatModelListener());
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
        uiComponent.addWindowListener(new MyWindowListener());
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

        int width = prefs.getInt("chatframe4.width", 700);
        int height = prefs.getInt("chatframe4.height", 500);
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
        uiComponent.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {
                getUIController().setActiveFrame(UIController.CHAT_FRAME_ID);
            }

            public void windowLostFocus(WindowEvent e) {
                // Ignore.
            }
        });
        uiComponent.setIconImage(Icons.getImageById(Icons.CHAT));
        uiComponent.setTitle(Translation.getTranslation("chat_frame.title"));
        uiComponent
            .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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
        if ((uiComponent.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
        {
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
     * @param autoSelect
     * @return
     */
    public ChatPanel displayChat(MemberInfo memberInfo, boolean autoSelect) {

        // Find existing session.
        for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals(memberInfo.nick)) {
                if (autoSelect) {
                    tabbedPane.setSelectedIndex(i);
                }
                clearMessagesIcon();
                return memberPanels.get(memberInfo);
            }
        }

        // New session.
        Member member = getController().getNodeManager().getNode(memberInfo);
        ChatPanel chatPanel = new ChatPanel(getController(), this, member);
        memberPanels.put(memberInfo, chatPanel);
        tabbedPane.addTab(memberInfo.nick, Icons.getIconFor(member), chatPanel
            .getUiComponent(), Translation.getTranslation(
            "chat_frame.tool_tip", member.getNick()));
        if (autoSelect) {
            tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
        }

        clearMessagesIcon();
        return chatPanel;
    }

    /**
     * Update the icons on the tabs as members come and go.
     * 
     * @param member
     */
    private void updateTabIcons(Member member) {
        if (!memberPanels.containsKey(member.getInfo())) {
            return;
        }
        for (MemberInfo memberInfo : memberPanels.keySet()) {
            if (member.getInfo().equals(memberInfo)) {
                Icon icon;
                if (newMessages.contains(member.getInfo())) {
                    icon = Icons.getIconById(Icons.CHAT_PENDING);
                } else {
                    icon = Icons.getIconFor(member);
                }
                Component component = memberPanels.get(memberInfo)
                    .getUiComponent();
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
        for (Iterator<MemberInfo> iter = memberPanels.keySet().iterator(); iter
            .hasNext();)
        {
            MemberInfo memberInfo = iter.next();
            if (memberInfo.equals(chatPartner.getInfo())) {
                Component component = memberPanels.get(memberInfo)
                    .getUiComponent();
                int count = tabbedPane.getComponentCount();
                for (int i = 0; i < count; i++) {
                    if (tabbedPane.getComponentAt(i).equals(component)) {
                        iter.remove();
                        tabbedPane.remove(i);
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

    private boolean showingTabForMember(MemberInfo info) {
        ChatPanel panel = memberPanels.get(info);
        if (panel != null) {
            Component component = tabbedPane.getSelectedComponent();
            return component == panel.getUiComponent();
        }
        return false;
    }

    /**
     * Handle all chat events. Diseminate to the required panel.
     * 
     * @param event
     */
    private void handleChatEvent(ChatModelEvent event) {
        if (event.getSource() instanceof Member) {
            Member source = (Member) event.getSource();

            // Okay, we are up.
            // Do we have a tab for this guy?
            ChatPanel panel = null;
            for (MemberInfo memberInfo : memberPanels.keySet()) {
                if (source.getInfo().equals(memberInfo)) {
                    // We have this guy.
                    panel = memberPanels.get(memberInfo);
                    break;
                }
            }

            // Message from someone new. Add panel.
            if (panel == null) {

                // Was this a status event? If so, we do not care.
                // Do not add a new chat panel just for a status event.
                if (event.isStatus()) {
                    return;
                }
                panel = displayChat(source.getInfo(), false);
            }

            if (!showingTabForMember(source.getInfo())) {
                newMessages.add(source.getInfo());
                updateTabIcons(source);
            }

            // Now display message.
            panel.updateChat();
        }

        // If this is not being displayed, show the pending messages button
        // in the UI, alerting the user to the messages.
        if (!getUIComponent().isVisible()) {
            getController().getUIController().showPendingMessages(true);
        }
    }

    /**
     * Clear the message icon for the currently selected tab.
     */
    private void clearMessagesIcon() {
        Component component = tabbedPane.getSelectedComponent();
        for (MemberInfo memberInfo : memberPanels.keySet()) {
            ChatPanel panel = memberPanels.get(memberInfo);
            if (panel.getUiComponent() == component) {
                newMessages.remove(memberInfo);
                Member member = getController().getNodeManager().getNode(
                    memberInfo);
                updateTabIcons(member);
            }
        }
    }

    // /////////////////
    // INNER CLASSES //
    // /////////////////

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

        public void nodeOffline(NodeManagerEvent e) {
            updateTabIcons(e.getNode());
        }

        public void nodeOnline(NodeManagerEvent e) {
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

    private class MyChatModelListener implements ChatModelListener {
        public void chatChanged(ChatModelEvent event) {
            handleChatEvent(event);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            // Avoid race for memberPanels
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    clearMessagesIcon();
                }
            });
        }
    }

    private class MyWindowListener extends WindowAdapter {

        // Clear the pending messages button in the UI, because user can
        // see the messages now.
        public void windowActivated(WindowEvent e) {
            getController().getUIController().showPendingMessages(false);
        }

        // Clear the pending messages button in the UI, because user can
        // see the messages now.
        public void windowOpened(WindowEvent e) {
            getController().getUIController().showPendingMessages(false);
        }
    }
}