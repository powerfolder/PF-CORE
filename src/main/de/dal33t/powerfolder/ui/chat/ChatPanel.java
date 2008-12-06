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
import javax.swing.text.StyledDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Class to show a chat session with a member.
 */
public class ChatPanel extends PFComponent {

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

    /**
     * Constructor
     *
     * @param controller
     */
    public ChatPanel(Controller controller, Member chatPartner) {
        super(controller);
        this.chatPartner = chatPartner;
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
        boolean connected = chatPartner.isCompleteyConnected();
        if (connected) {
            chatInput.setBackground(Color.WHITE);
        } else {
            chatInput.setBackground(Color.LIGHT_GRAY);
        }
        chatInput.setEnabled(connected);
    }

    private void updateChat() {
        ChatLine[] lines = getController().getUIController().getChatModel().getChatText(chatPartner);
        if (lines != null) {
            updateChat(lines);
        }
    }

    /**
     * called if a chatline is recieved, completely replaces the text in the
     * output field.
     */
    void updateChat(ChatLine[] lines) {
        final StyledDocument doc = new DefaultStyledDocument();
        createStyles(doc);
        try {
            for (int i = 0; i < lines.length; i++) {
                ChatLine line = lines[i];
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
                    Controller controller = getController();
                    ChatModel chatModel = controller.getUIController()
                            .getChatModel();
                    if (chatPartner.isCompleteyConnected()) {
                        chatModel.addChatLine(
                                chatPartner, controller.getMySelf(), message);
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
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().equals(chatPartner)) {
                updateInputField();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (e.getNode().equals(chatPartner)) {
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

}
