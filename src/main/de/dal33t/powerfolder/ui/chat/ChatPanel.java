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

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.MemberChatMessage;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Class to show a chat session with a member.
 */
public class ChatPanel extends PFUIComponent {

    // styles used in StyledDocument
    private static final String NORMAL = "normal";
    private static final String BOLD = "bold";
    private static final String BOLD_BLUE = "bold-blue";
    private static final String BOLD_GREEN = "bold-green";

    private JPanel uiComponent;
    private JTextArea chatInput;
    private JTextPane chatOutput;
    private JScrollPane outputScrollPane;
    private JScrollPane inputScrollPane;
    private JPanel toolBar;
    private Member chatPartner;
    private ChatFrame chatFrame;

    private JButton addRemoveButton;

    /**
     * Constructor
     *
     * @param controller
     */
    public ChatPanel(Controller controller, ChatFrame chatFrame,
                     Member chatPartner) {
        super(controller);
        this.chatPartner = chatPartner;
        this.chatFrame = chatFrame;
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        controller.getUIController().getChatModel().addChatModelListener(
            new MyChatModelListener());
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

        addRemoveButton = new JButton(getApplicationModel().getActionModel()
                .getAddFriendAction());
        addRemoveButton.addActionListener(new MyAddRemoveActionListener());
        configureAddRemoveButton();

        JButton reconnectButton = new JButton(new MyReconnectAction(getController()));

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(addRemoveButton);
        bar.addRelatedGap();
        bar.addGridded(reconnectButton);

        bar.getPanel();

        JButton closeButton = new JButton3Icons(
                Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL,
                Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER,
                Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH);
        closeButton.setToolTipText(Translation.getTranslation(
                "chat_panel.close_button.tool_tip", chatPartner.getNick()));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeSession();
            }
        });

        FormLayout layout = new FormLayout("fill:0:grow, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(bar.getPanel(), cc.xy(1, 1));
        builder.add(closeButton, cc.xy(2, 1));
        toolBar = builder.getPanel(); 
    }

    /**
     * Close this tab in the parent frame.
     */
    private void closeSession() {
        chatFrame.closeSession(chatPartner);
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
        boolean connected = chatPartner.isCompleteyConnected();
        if (connected) {
            chatInput.setBackground(Color.WHITE);
        } else {
            chatInput.setBackground(Color.LIGHT_GRAY);
        }
        chatInput.setEnabled(connected);
    }

    /**
     * Update the chat lines if for me.
     */
    private void updateChat() {
        ChatLine[] lines = getUIController().getChatModel().getChatText(chatPartner);
        if (lines != null) {
            updateChat(lines);
        }
    }

    /**
     * Called if a chatline is recieved, completely replaces the text in the
     * output field.
     */
    void updateChat(ChatLine[] lines) {
        final StyledDocument doc = new DefaultStyledDocument();
        createStyles(doc);
        try {
            for (ChatLine line : lines) {
                if (line.isStatus()) {
                    doc.insertString(doc.getLength(), "* " + line.getText(),
                            doc.getStyle(BOLD_GREEN));

                } else {
                    Member otherMember = line.getFromMember();
                    String text = line.getText();
                    if (otherMember.isMySelf()) {
                        doc.insertString(doc.getLength(),
                            otherMember.getNick(), doc.getStyle(BOLD_BLUE));
                        doc.insertString(doc.getLength(), ": " + text, doc
                            .getStyle(NORMAL));
                    } else {
                        doc.insertString(doc.getLength(),
                            otherMember.getNick(), doc.getStyle(BOLD));
                        doc.insertString(doc.getLength(), ": " + text, doc
                            .getStyle(NORMAL));
                    }
                }
            }
        } catch (BadLocationException ble) {
            logWarning("Could not set chat text", ble);
        }

        Runnable runner = new Runnable() {
            public void run() {
                chatOutput.setStyledDocument(doc);

                // Make sure first text is at bottom
                while (chatOutput.getPreferredSize().height <
                        chatOutput.getSize().height) {
                    try {
                        doc.insertString(0, "\n", null);
                    } catch (BadLocationException ble) {
                        logWarning("Could not set chat text", ble);
                    }
                }

                Rectangle r = new Rectangle(1,
                        chatOutput.getPreferredSize().height, 1, 1);

                // Make sure the last line of text is visible
                outputScrollPane.getViewport().scrollRectToVisible(r);

                // Make sure we can always type text in the input
                chatInput.requestFocusInWindow();

                updateInputField();
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * Creates the document styles.
     *
     * @param doc
     */
    private static void createStyles(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(
            StyleContext.DEFAULT_STYLE);

        doc.addStyle(NORMAL, def);

        Style s = doc.addStyle(BOLD, def);
        StyleConstants.setBold(s, true);

        s = doc.addStyle(BOLD_BLUE, def);
        StyleConstants.setForeground(s, Color.BLUE);
        StyleConstants.setBold(s, true);

        s = doc.addStyle(BOLD_GREEN, def);
        StyleConstants.setForeground(s, Color.GREEN.darker().darker());
        StyleConstants.setBold(s, true);
    }

    /**
     * Configure the buttone to be add or remove.
     */
    private void configureAddRemoveButton() {
        if (chatPartner.isFriend()) {
            addRemoveButton.setAction(getApplicationModel().getActionModel()
                    .getRemoveFriendAction());
        } else {
            addRemoveButton.setAction(getApplicationModel().getActionModel()
                    .getAddFriendAction());
        }
    }

    ///////////////////
    // INNER CLASSES //
    ///////////////////

    /**
     * Key listener to send messages on entere key.
     */
    private class MyKeyListener  extends KeyAdapter {
        public void keyTyped(KeyEvent e) {
            char keyTyped = e.getKeyChar();
            if (keyTyped == '\n') { // enter key = send message
                String message = chatInput.getText();
                if (message.trim().length() > 0) { // no SPAM on "enter"
                    ChatModel chatModel = getUIController()
                            .getChatModel();
                    if (chatPartner.isCompleteyConnected()) {
                        chatModel.addChatLine(
                                chatPartner, getController().getMySelf(), message);
                        chatInput.setText("");
                        MemberChatMessage chatMessage = new MemberChatMessage(
                            message);
                        chatPartner.sendMessageAsynchron(chatMessage,
                                "chat line not sent");
                    } else {
                        chatModel.addStatusChatLine(chatPartner,
                            Translation.getTranslation("chat_panel.cannot_deliver",
                                    chatPartner.getNick()));
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
            updateOnNodeChange(e);
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void friendAdded(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void startStop(NodeManagerEvent e) {
        }

        private void updateOnNodeChange(NodeManagerEvent e) {
            if (e.getNode().equals(chatPartner)) {
                updateInputField();
                configureAddRemoveButton();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /** Updates the chat if the current chat is changed */
    private class MyChatModelListener implements ChatModelListener {
        /**
         * Called from the model if a message (about a Member) is
         * received from other member or typed by this member (myself)
         */
        public void chatChanged(ChatModelEvent event) {
            Object source = event.getSource();
            if (source instanceof Member) {
                Member eventMember = (Member) source;
                if (chatPartner != null && chatPartner.equals(eventMember)) {
                    updateChat();
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Class to listen for add / remove friendship requests.
     */
    private class MyAddRemoveActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(chatPartner.getInfo(),
                    e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
            if (chatPartner.isFriend()) {
                getApplicationModel().getActionModel().getRemoveFriendAction()
                        .actionPerformed(ae);
            } else {
                getApplicationModel().getActionModel().getAddFriendAction()
                        .actionPerformed(ae);
            }
        }
    }


    private class MyReconnectAction extends BaseAction {

        MyReconnectAction(Controller controller) {
            super("action_reconnect", controller);
        }

        public void actionPerformed(ActionEvent e) {

            // Build new connect dialog
            final ConnectDialog connectDialog = new ConnectDialog(getController());

            Runnable connector = new Runnable() {
                public void run() {

                    // Open connect dialog if ui is open
                    connectDialog.open(chatPartner.getNick());

                    // Close connection first
                    chatPartner.shutdown();

                    // Now execute the connect
                    try {
                        if (!chatPartner.reconnect()) {
                            throw new ConnectionException(Translation.getTranslation(
                                    "dialog.unable_to_connect_to_member",
                                chatPartner.getNick()));
                        }
                    } catch (ConnectionException ex) {
                        connectDialog.close();
                        if (!connectDialog.isCanceled() && !chatPartner.isConnected()) {
                            // Show if user didn't cancel
                            ex.show(getController());
                        }
                    }

                    // Close dialog
                    connectDialog.close();
                }
            };

            // Start connect in anonymous thread
            new Thread(connector, "Reconnector to " + chatPartner.getNick()).start();
        }
    }

}
