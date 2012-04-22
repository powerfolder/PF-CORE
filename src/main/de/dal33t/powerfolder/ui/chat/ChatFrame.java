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

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.UIConstants;
import de.dal33t.powerfolder.util.Translation;

/**
 * The information window.
 */
public class ChatFrame extends PFUIComponent {

    private static final String CHATFRAME_X = "chatframe.x";
    private static final String CHATFRAME_Y = "chatframe.y";
    private static final String CHATFRAME_WIDTH = "chatframe.width";
    private static final String CHATFRAME_HEIGHT = "chatframe.height";
    private static final String CHATFRAME_MAXIMIZED = "chatframe.maximized";
    private static final String CHATFRAME_SET = "chatframe.set";

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
        return uiComponent;
    }

    /**
     * Builds the UI component.
     */
    private void buildUIComponent() {
        Preferences prefs = getController().getPreferences();

        // Pack elements
        uiComponent.pack();

        if (prefs.getBoolean(CHATFRAME_SET, false)) {

            uiComponent.setLocation(prefs.getInt(CHATFRAME_X,
                    UIConstants.DEFAULT_FRAME_X),
                    prefs.getInt(CHATFRAME_Y, UIConstants.DEFAULT_FRAME_Y));
            uiComponent.setSize(prefs.getInt(CHATFRAME_WIDTH,
                    UIConstants.DEFAULT_FRAME_WIDTH),
                    prefs.getInt(CHATFRAME_HEIGHT,
                            UIConstants.DEFAULT_FRAME_HEIGHT));

            if (prefs.getBoolean(CHATFRAME_MAXIMIZED,
                    UIConstants.DEFAULT_FRAME_MAXIMIZED)) {
                uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        } else {

            // First time displayed, use sensible defaults.
            getUIComponent().setLocation(UIConstants.DEFAULT_FRAME_X,
                    UIConstants.DEFAULT_FRAME_Y);
            getUIComponent().setSize(UIConstants.DEFAULT_FRAME_WIDTH,
                    UIConstants.DEFAULT_FRAME_HEIGHT);
            getUIComponent().setExtendedState(
                    UIConstants.DEFAULT_FRAME_EXTENDED_STATE);
            prefs.putBoolean(CHATFRAME_SET, true);
        }

        UIUtil.putOnScreen(uiComponent);

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
            prefs.putBoolean(CHATFRAME_MAXIMIZED, true);
        } else {
            prefs.putInt(CHATFRAME_X, uiComponent.getX());
            prefs.putInt(CHATFRAME_WIDTH, uiComponent.getWidth());
            prefs.putInt(CHATFRAME_Y, uiComponent.getY());
            prefs.putInt(CHATFRAME_HEIGHT, uiComponent.getHeight());
            prefs.putBoolean(CHATFRAME_MAXIMIZED, false);
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

    private void handleChatAdviceEvent(ChatAdviceEvent event) {
        // Do I care? Is this advice for one of my members?
        for (Map.Entry<MemberInfo, ChatPanel> entry : memberPanels.entrySet()) {
            Member source = (Member) event.getSource();
            if (source.getInfo().equals(entry.getKey())) {
                entry.getValue().adviseChat(source.getNick());
            }
        }
    }

    /**
     * Handle all chat events. Diseminate to the required panel.
     * 
     * @param event
     */
    private void handleChatModelEvent(ChatModelEvent event) {
        if (event.getSource() instanceof Member) {
            Member fromMember = (Member) event.getSource();

            // Do we have a tab for this member?
            ChatPanel panel = null;
            for (MemberInfo memberInfo : memberPanels.keySet()) {
                if (fromMember.getInfo().equals(memberInfo)) {
                    // We have this guy.
                    panel = memberPanels.get(memberInfo);
                    break;
                }
            }

            // Message from someone new. Add panel.
            if (panel == null) {

                // Was this a status event? If so, we do not care.
                // Do not add a new chat panel just for a status event.
                if (event.isStatusFlag()) {
                    return;
                }
                panel = displayChat(fromMember.getInfo(), false);
            }

            if (!showingTabForMember(fromMember.getInfo())) {
                newMessages.add(fromMember.getInfo());
                updateTabIcons(fromMember);
            }

            // Now display message.
            panel.updateChat();
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
    private class MyNodeManagerListener extends NodeManagerAdapter {

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

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MyChatModelListener implements ChatModelListener {
        public void chatChanged(ChatModelEvent event) {
            handleChatModelEvent(event);
        }

        public void chatAdvice(ChatAdviceEvent event) {
            handleChatAdviceEvent(event);
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

                    Component component = tabbedPane.getSelectedComponent();
                    for (ChatPanel chatPanel : memberPanels.values()) {
                        if (component == chatPanel.getUiComponent()) {
                            chatPanel.setInputFocus();
                        }
                    }
                }
            });
        }
    }
}