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
* $Id: ChatPanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.chat;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.message.MemberChatMessage;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Class to show a chat session with a member.
 */
public class ChatPanel extends PFComponent {

    private JPanel uiComponent;
    private JTextArea chatInput;
    private JTextPane chatOutput;
    private JScrollPane outputScrollPane;
    private JScrollPane inputScrollPane;
    private JPanel toolBar;
    private Member member;

    /**
     * Constructor
     *
     * @param controller
     */
    public ChatPanel(Controller controller, Member member) {
        super(controller);
        this.member = member;
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());        
    }

    /**
     * Create the ui if required and return.
     *
     * @return
     */
    public JPanel getUiComponent() {
        if (uiComponent == null) {
            initialize();
            buildUiComponent();
        }
        return uiComponent;
    }

    /**
     * Build the ui.
     */
    private void buildUiComponent() {

        FormLayout layout = new FormLayout("3dlu, fill:0:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow, 3dlu, pref, 3dlu, pref, 3dlu");
        //         tools       sep         me                 sep         you

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(toolBar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xy(2, 4));
        builder.add(outputScrollPane, cc.xy(2, 6));
        builder.addSeparator(null, cc.xy(2, 8));
        builder.add(inputScrollPane, cc.xy(2, 10));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the ui.
     */
    private void initialize() {
        createToolBar();
        createOutputComponents();
        createInputComponents();
    }

    /**
     * Create the tool bar.
     */
    private void createToolBar() {
        toolBar = new JPanel();
    }

    /**
     * Create the output components.
     */
    private void createOutputComponents() {
        chatOutput = new JTextPane();
        chatOutput.setEditable(false);
        outputScrollPane = new JScrollPane(chatOutput);
        chatOutput.getParent().setBackground(Color.WHITE);
        UIUtil.removeBorder(outputScrollPane);
    }

    /**
     * Create the input components.
     */
    private void createInputComponents() {
        chatInput = new JTextArea(5, 30);
        chatInput.setEditable(true);
        chatInput.addKeyListener(new MyKeyListener());
        inputScrollPane = new JScrollPane(chatInput);
        UIUtil.removeBorder(inputScrollPane);
        updateInputField();
    }

    /**
     * Updates the input field state (enabled/disabled) accoriding the the chat
     * partner
     */
    public void updateInputField() {
        boolean connected = member.isCompleteyConnected();
        if (connected) {
            chatInput.setBackground(Color.WHITE);
        } else {
            chatInput.setBackground(Color.LIGHT_GRAY);
        }
        chatInput.setEnabled(connected);
    }

    /**
     * Key listener to send messages on entere key.
     */
    private class MyKeyListener  extends KeyAdapter {
        public void keyTyped(KeyEvent e) {
            char keyTyped = e.getKeyChar();
            if (keyTyped == '\n') { // enter key = send message
                String message = chatInput.getText();
                if (message.trim().length() > 0) { // no SPAM on "enter"
                    Controller controller = getController();
                    ChatModel chatModel = controller.getUIController()
                            .getChatModel();
                    if (member.isCompleteyConnected()) {
                        chatModel.addChatLine(
                            member, controller.getMySelf(), message);
                        chatInput.setText("");
                        MemberChatMessage chatMessage = new MemberChatMessage(
                            message);
                        member.sendMessageAsynchron(chatMessage,
                                "chat line not sent");
                    } else {
                        chatModel.addStatusChatLine(member,
                            Translation.getTranslation("chat_panel.cannot_deliver",
                                    member.getNick()));
                    }

                } else { // Enter key without text - clear.
                    chatInput.setText("");
                    chatInput.requestFocusInWindow();
                }
            }
            // Update input field
            updateInputField();

        }
    }

    /**
     * Listener to NodeManager. Listens on changes in the online state and
     * update the ui components according to that
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().equals(member)) {
                updateInputField();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (e.getNode().equals(member)) {
                updateInputField();
            }
        }

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}
