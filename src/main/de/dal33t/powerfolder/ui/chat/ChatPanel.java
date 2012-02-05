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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.message.MemberChatMessage;
import de.dal33t.powerfolder.message.MemberChatAdvice;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.ui.util.*;

/**
 * Class to show a chat session with a member.
 */
public class ChatPanel extends PFUIComponent {

    // styles used in StyledDocument
    private static final String NORMAL = "normal";
    private static final String BOLD = "bold";
    private static final String ITALIC = "italic";
    private static final String UNDERLINE = "underline";
    private static final String BOLD_ITALIC = "bold-italic";
    private static final String BOLD_UNDERLINE = "bold-underline";
    private static final String ITALIC_UNDERLINE = "italic-underline";
    private static final String BOLD_ITALIC_UNDERLINE = "bold-italic-underline";
    private static final String BOLD_BLUE = "bold-blue";
    private static final String BOLD_GREEN = "bold-green";

    private JPanel uiComponent;
    private JTextArea chatInput;
    private JTextPane chatOutput;
    private JScrollPane outputScrollPane;
    private JScrollPane inputScrollPane;
    private JLabel chatAdviceLabel;
    private JPanel toolBar;
    private Member chatPartner;
    private ChatFrame chatFrame;
    private MyAddRemoveFriendAction addRemoveFriendAction;
    private ChatModel chatModel;
    private JPopupMenu outputContextMenu;
    private JPopupMenu inputContextMenu;
    private MyCopyAction copyAction;
    private MyPasteAction pasteAction;
    private MySelectAllOutputAction selectAllOutputAction;
    private MySelectAllInputAction selectAllInputAction;
    private MyBoldAction boldAction;
    private MyItalicAction italicAction;
    private MyUnderlineAction underlineAction;
    private final AtomicInteger chatCharacterCounter;

    /**
     * Constructor NOTE: This panel is NOT responsible for receiving messages.
     * That is handled by the ChatFrame.
     * 
     * @param controller
     */
    public ChatPanel(Controller controller, ChatFrame chatFrame,
        Member chatPartner)
    {
        super(controller);
        chatModel = controller.getUIController().getApplicationModel()
            .getChatModel();
        this.chatPartner = chatPartner;
        this.chatFrame = chatFrame;
        chatCharacterCounter = new AtomicInteger();
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
        // tools sep me sep you

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(toolBar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xy(2, 4));
        builder.add(outputScrollPane, cc.xy(2, 6));
        builder.addSeparator(null, cc.xy(2, 8));
        builder.add(inputScrollPane, cc.xy(2, 10));
        uiComponent = builder.getPanel();
        if (chatInput.isEnabled()) {
            chatInput.requestFocusInWindow();
        }
    }

    /**
     * Initialize the ui.
     */
    private void initialize() {
        copyAction = new MyCopyAction(getController());
        pasteAction = new MyPasteAction(getController());
        selectAllOutputAction = new MySelectAllOutputAction(getController());
        selectAllInputAction = new MySelectAllInputAction(getController());
        boldAction = new MyBoldAction(getController());
        italicAction = new MyItalicAction(getController());
        underlineAction = new MyUnderlineAction(getController());
        chatAdviceLabel = new JLabel();
        createToolBar();
        createOutputComponents();
        createInputComponents();
    }

    /**
     * Create the tool bar.
     */
    private void createToolBar() {

        addRemoveFriendAction = new MyAddRemoveFriendAction(getController());

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        configureAddRemoveAction();

        bar.addGridded(new JButton(new MyReconnectAction(getController())));
        bar.getPanel();

        JButton closeButton = new JButton3Icons(Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        closeButton.setToolTipText(Translation.getTranslation(
            "chat_panel.close_button.tool_tip", chatPartner.getNick()));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeSession();
            }
        });

        FormLayout layout = new FormLayout(
            "pref, 3dlu, fill:0:grow, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(bar.getPanel(), cc.xy(1, 1));
        builder.add(chatAdviceLabel, cc.xy(3, 1));
        builder.add(closeButton, cc.xy(5, 1));
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
        chatOutput.addMouseListener(new MyOutputMouseListener());
        chatOutput.addCaretListener(new MyOutputCaretListener());
        UIUtil.removeBorder(outputScrollPane);
    }

    /**
     * Create the input components.
     */
    private void createInputComponents() {
        chatInput = new JTextArea(5, 30);
        chatInput.setEditable(true);
        chatInput.addKeyListener(new MyInputKeyListener());
        chatInput.addMouseListener(new MyInputMouseListener());
        chatInput.addCaretListener(new MyInputCaretListener());
        inputScrollPane = new JScrollPane(chatInput);
        UIUtil.removeBorder(inputScrollPane);
        updateInputField();
    }

    /**
     * Updates the input field state (enabled/disabled) accoriding the the chat
     * partner
     */
    public void updateInputField() {
        boolean connected = chatPartner.isCompletelyConnected();
        if (connected) {
            chatInput.setBackground(Color.WHITE);
        } else {
            chatInput.setBackground(Color.LIGHT_GRAY);
        }
        chatInput.setEnabled(connected);
        if (connected) {
            chatInput.requestFocusInWindow();
        }
    }

    /**
     * Update the chat lines if for me.
     */
    public void updateChat() {
        ChatLine[] lines = chatModel.getChatText(chatPartner);
        if (lines != null) {
            updateChat(lines);
        }
    }

    /**
     * Called if a chatline is recieved, completely replaces the text in the
     * output field.
     */
    private void updateChat(ChatLine[] lines) {

        // Clear any existing chat advice.
        chatAdviceLabel.setText("");

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
                    } else {
                        doc.insertString(doc.getLength(),
                            otherMember.getNick(), doc.getStyle(BOLD));
                    }
                    doc.insertString(doc.getLength(), ": ", doc
                        .getStyle(NORMAL));
                    List<DocumentSection> sections = parseText(text);
                    for (DocumentSection section : sections) {
                        doc.insertString(doc.getLength(), section.getText(),
                            doc.getStyle(section.getType()));
                    }
                }
            }
        } catch (BadLocationException ble) {
            logWarning("Could not set chat text", ble);
        }

        Runnable runner = new Runnable() {
            public void run() {
                chatOutput.setStyledDocument(doc);

                // This looks like a big hack(tm):
                // Make sure first text is at bottom
                int n = 0;
                while (chatOutput.getPreferredSize().height < chatOutput
                    .getSize().height
                    && n < 200)
                {
                    n++;
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
     * Split text into bold, italic and underline sections.
     * 
     * @param text
     * @return
     */
    private static List<DocumentSection> parseText(String text) {
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        StringBuilder sb = new StringBuilder();
        List<DocumentSection> sections = new ArrayList<DocumentSection>();
        for (int i = 0; i < text.length(); i++) {
            if (i < text.length() - 3
                && (text.substring(i, i + 3).equals("<B>") || text.substring(i,
                    i + 3).equals("<b>")))
            {
                doChunk(sb, sections, bold, italic, underline);
                bold = true;
                i += 2;
            } else if (i < text.length() - 3
                && (text.substring(i, i + 3).equals("<I>") || text.substring(i,
                    i + 3).equals("<i>")))
            {
                doChunk(sb, sections, bold, italic, underline);
                italic = true;
                i += 2;
            } else if (i < text.length() - 3
                && (text.substring(i, i + 3).equals("<U>") || text.substring(i,
                    i + 3).equals("<u>")))
            {
                doChunk(sb, sections, bold, italic, underline);
                underline = true;
                i += 2;
            } else if (i < text.length() - 4
                && (text.substring(i, i + 4).equals("</B>") || text.substring(
                    i, i + 4).equals("</b>")) && bold)
            {
                doChunk(sb, sections, bold, italic, underline);
                bold = false;
                i += 3;
            } else if (i < text.length() - 4
                && (text.substring(i, i + 4).equals("</I>") || text.substring(
                    i, i + 4).equals("</i>")) && italic)
            {
                doChunk(sb, sections, bold, italic, underline);
                italic = false;
                i += 3;
            } else if (i < text.length() - 4
                && (text.substring(i, i + 4).equals("</U>") || text.substring(
                    i, i + 4).equals("</u>")) && underline)
            {
                doChunk(sb, sections, bold, italic, underline);
                underline = false;
                i += 3;
            } else {
                sb.append(text.charAt(i));
            }
        }
        doChunk(sb, sections, bold, italic, underline);
        return sections;
    }

    private static void doChunk(StringBuilder sb,
        List<DocumentSection> sections, boolean bold, boolean italic,
        boolean underline)
    {
        String type;
        if (bold) {
            if (italic) {
                if (underline) {
                    type = BOLD_ITALIC_UNDERLINE;
                } else {
                    type = BOLD_ITALIC;
                }
            } else {
                if (underline) {
                    type = BOLD_UNDERLINE;
                } else {
                    type = BOLD;
                }
            }
        } else {
            if (italic) {
                if (underline) {
                    type = ITALIC_UNDERLINE;
                } else {
                    type = ITALIC;
                }
            } else {
                if (underline) {
                    type = UNDERLINE;
                } else {
                    type = NORMAL;
                }
            }
        }
        DocumentSection ds = new DocumentSection(type, sb.toString());
        sections.add(ds);
        sb.delete(0, sb.length());
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

        s = doc.addStyle(ITALIC, def);
        StyleConstants.setItalic(s, true);

        s = doc.addStyle(UNDERLINE, def);
        StyleConstants.setUnderline(s, true);

        s = doc.addStyle(BOLD_ITALIC, def);
        StyleConstants.setBold(s, true);
        StyleConstants.setItalic(s, true);

        s = doc.addStyle(ITALIC_UNDERLINE, def);
        StyleConstants.setItalic(s, true);
        StyleConstants.setUnderline(s, true);

        s = doc.addStyle(BOLD_UNDERLINE, def);
        StyleConstants.setBold(s, true);
        StyleConstants.setUnderline(s, true);

        s = doc.addStyle(BOLD_ITALIC_UNDERLINE, def);
        StyleConstants.setBold(s, true);
        StyleConstants.setItalic(s, true);
        StyleConstants.setUnderline(s, true);

        s = doc.addStyle(BOLD_BLUE, def);
        StyleConstants.setForeground(s, Color.BLUE);
        StyleConstants.setBold(s, true);

        s = doc.addStyle(BOLD_GREEN, def);
        StyleConstants.setForeground(s, Color.GREEN.darker().darker());
        StyleConstants.setBold(s, true);
    }

    /**
     * Configure the button to be add or remove.
     */
    private void configureAddRemoveAction() {
        if (chatPartner.isFriend()) {
            addRemoveFriendAction.setAdd(false);
        } else {
            addRemoveFriendAction.setAdd(true);
        }
    }

    public void setInputFocus() {
        chatInput.requestFocusInWindow();
    }

    /**
     * Convert something like "qwer<B>asdf</B>zxcv" to "qwerasdfzxcv" and back.
     * mark| |dot mark| |dot
     * 
     * @param textArea
     * @param token
     */
    private static void toggle(JTextArea textArea, String token) {
        int mark = textArea.getCaret().getMark();
        int dot = textArea.getCaret().getDot();
        if (mark > dot) {
            int temp = mark;
            mark = dot;
            dot = temp;
        }
        String text = textArea.getText();
        String pre = text.substring(0, mark);
        String in = text.substring(mark, dot);
        String post = text.substring(dot);
        String frontToken = '<' + token + '>';
        String backToken = "</" + token + '>';
        if (in.startsWith(frontToken) && in.endsWith(backToken)) {
            // Remove formatting inclusive
            in = in.substring(3).substring(0, in.length() - 7);
            textArea.setText(pre + in + post);
        } else if (pre.endsWith(frontToken) && post.startsWith(backToken)) {
            // Remove formatting exclusive
            pre = pre.substring(0, pre.length() - 3);
            post = post.substring(4);
            textArea.setText(pre + in + post);
        } else {
            // Add formatting
            in = frontToken + in + backToken;
            textArea.setText(pre + in + post);
        }
    }

    /**
     * This is advice that the chat partner is typing a chat message.
     */
    public void adviseChat(String nick) {
        chatAdviceLabel.setText(Translation.getTranslation(
            "chat_panel.chat_advice.text", nick));
        getController().getThreadPool().schedule(new Runnable() {
            public void run() {
                chatAdviceLabel.setText("");
            }
        }, 3, TimeUnit.SECONDS);
    }

    // ////////////////
    // INNER CLASSES //
    // ////////////////

    /**
     * Key listener to send messages on enter key.
     */
    private class MyInputKeyListener extends KeyAdapter {
        public void keyTyped(KeyEvent e) {
            char keyTyped = e.getKeyChar();
            if (keyTyped == '\n') { // enter key = send message

                // Reset counter on "enter".
                chatCharacterCounter.set(0);

                String message = chatInput.getText();
                if (message.trim().length() > 0) { // no SPAM on "enter"

                    if (chatPartner.isCompletelyConnected()) {
                        chatModel.addChatLine(chatPartner, getController()
                            .getMySelf(), message, true);
                        chatInput.setText("");
                        MemberChatMessage chatMessage = new MemberChatMessage(
                            message);
                        chatPartner.sendMessageAsynchron(chatMessage);
                    } else {
                        chatModel.addStatusChatLine(chatPartner, Translation
                            .getTranslation("chat_panel.cannot_deliver",
                                chatPartner.getNick()), false);
                    }

                } else { // Enter key without text - clear.
                    chatInput.setText("");
                    chatInput.requestFocusInWindow();
                }
            } else {
                // Send a MemberChatAdvice every 10 characters
                int count = chatCharacterCounter.getAndIncrement();
                if (count >= 10) {
                    chatCharacterCounter.set(1);
                    count = 0;
                }
                if (count == 0) {
                    chatPartner.sendMessageAsynchron(new MemberChatAdvice());
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
    private class MyNodeManagerListener extends NodeManagerAdapter {

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

        public void nodeOnline(NodeManagerEvent e) {
            updateOnNodeChange(e);
        }

        public void nodeOffline(NodeManagerEvent e) {
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

        private void updateOnNodeChange(NodeManagerEvent e) {
            if (e.getNode().equals(chatPartner)) {
                // #1816 intermittant bug, possibly caused by accessing fields
                // before they are initialized ? So ensure UI exists first.
                getUiComponent();

                updateInputField();
                configureAddRemoveAction();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyReconnectAction extends BaseAction {

        MyReconnectAction(Controller controller) {
            super("action_reconnect", controller);
            setIcon(null);
        }

        public void actionPerformed(ActionEvent e) {

            // Build new connect dialog
            final ConnectDialog connectDialog = new ConnectDialog(
                getController(), UIUtil.getParentWindow(e));

            Runnable connector = new Runnable() {
                public void run() {

                    // Open connect dialog if ui is open
                    connectDialog.open(chatPartner.getNick());

                    // Close connection first
                    chatPartner.shutdown();

                    // Now execute the connect
                    try {
                        if (chatPartner.reconnect().isFailure()) {
                            throw new ConnectionException(Translation
                                .getTranslation(
                                    "dialog.unable_to_connect_to_member",
                                    chatPartner.getNick()));
                        }
                    } catch (ConnectionException ex) {
                        connectDialog.close();
                        if (!connectDialog.isCanceled()
                            && !chatPartner.isConnected())
                        {
                            // Show if user didn't cancel
                            ex.show(getController());
                        }
                    }

                    // Close dialog
                    connectDialog.close();
                }
            };

            // Start connect in anonymous thread
            new Thread(connector, "Reconnector to " + chatPartner.getNick())
                .start();
        }
    }

    private class MyAddRemoveFriendAction extends BaseAction {

        private boolean add = true;

        private MyAddRemoveFriendAction(Controller controller) {
            super("action_add_friend", controller);
            setIcon(null);
        }

        public void setAdd(boolean add) {
            this.add = add;
            if (add) {
                configureFromActionId("action_add_friend");
            } else {
                configureFromActionId("action_remove_friend");
            }
            setIcon(null);
        }

        public void actionPerformed(ActionEvent e) {
            if (add) {
                boolean askForFriendshipMessage = PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE
                    .getValueBoolean(getController());
                if (askForFriendshipMessage) {

                    // Prompt for personal message.
                    String[] options = {
                        Translation.getTranslation("general.ok"),
                        Translation.getTranslation("general.cancel")};

                    FormLayout layout = new FormLayout("pref",
                        "pref, 3dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    String nick = chatPartner.getNick();
                    String text = Translation.getTranslation(
                        "friend.search.personal.message.text2", nick);
                    builder.add(new JLabel(text), cc.xy(1, 1));
                    JTextArea textArea = new JTextArea();
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 3));
                    JPanel innerPanel = builder.getPanel();

                    NeverAskAgainResponse response = DialogFactory
                        .genericDialog(
                            getController(),
                            Translation
                                .getTranslation("friend.search.personal.message.title"),
                            innerPanel, options, 0, GenericDialogType.INFO,
                            Translation.getTranslation("general.neverAskAgain"));
                    if (response.getButtonIndex() == 0) { // == OK
                        String personalMessage = textArea.getText();
                        chatPartner.setFriend(true, personalMessage);
                    }
                    if (response.isNeverAskAgain()) {
                        // don't ask me again
                        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE.setValue(
                            getController(), false);
                    }
                } else {
                    // Send with no personal messages
                    chatPartner.setFriend(true, null);
                }
            } else {
                chatPartner.setFriend(false, null);
            }
        }
    }

    private class MyOutputMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showOutputContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showOutputContextMenu(e);
            }
        }

        private void showOutputContextMenu(final MouseEvent evt) {
            chatOutput.requestFocus();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createOutputPopupMenu().show(evt.getComponent(),
                        evt.getX(), evt.getY());
                }
            });
        }

        public JPopupMenu createOutputPopupMenu() {
            if (outputContextMenu == null) {
                outputContextMenu = new JPopupMenu();
                outputContextMenu.add(copyAction);
                outputContextMenu.add(selectAllOutputAction);
            }
            return outputContextMenu;
        }
    }

    private class MyInputMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showInputContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showInputContextMenu(e);
            }
        }

        private void showInputContextMenu(final MouseEvent evt) {
            chatInput.requestFocus();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createInputPopupMenu().show(evt.getComponent(), evt.getX(),
                        evt.getY());
                }
            });
        }

        public JPopupMenu createInputPopupMenu() {
            if (inputContextMenu == null) {
                inputContextMenu = new JPopupMenu();
                inputContextMenu.add(boldAction);
                inputContextMenu.add(italicAction);
                inputContextMenu.add(underlineAction);
                inputContextMenu.addSeparator();
                inputContextMenu.add(pasteAction);
                inputContextMenu.add(selectAllInputAction);
            }
            return inputContextMenu;
        }
    }

    private class MyCopyAction extends BaseAction {

        private MyCopyAction(Controller controller) {
            super("action_copy", controller);
        }

        public void actionPerformed(ActionEvent e) {
            Util.setClipboardContents(chatOutput.getSelectedText().trim());
        }
    }

    private class MyPasteAction extends BaseAction {

        private MyPasteAction(Controller controller) {
            super("action_paste", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int start = chatInput.getCaretPosition();
            chatInput.insert(Util.getClipboardContents().trim(), start);
        }
    }

    private class MySelectAllInputAction extends BaseAction {

        private MySelectAllInputAction(Controller controller) {
            super("action_select_all", controller);
        }

        public void actionPerformed(ActionEvent e) {
            chatInput.selectAll();
        }
    }

    private class MySelectAllOutputAction extends BaseAction {

        private MySelectAllOutputAction(Controller controller) {
            super("action_select_all", controller);
        }

        public void actionPerformed(ActionEvent e) {
            chatOutput.selectAll();
        }
    }

    private class MyOutputCaretListener implements CaretListener {
        public void caretUpdate(CaretEvent e) {
            copyAction.setEnabled(e.getDot() != e.getMark());
        }
    }

    private class MyInputCaretListener implements CaretListener {
        public void caretUpdate(CaretEvent e) {
            boldAction.setEnabled(e.getDot() != e.getMark());
            italicAction.setEnabled(e.getDot() != e.getMark());
            underlineAction.setEnabled(e.getDot() != e.getMark());
        }
    }

    private class MyBoldAction extends BaseAction {

        private MyBoldAction(Controller controller) {
            super("action_bold", controller);
        }

        public void actionPerformed(ActionEvent e) {
            toggle(chatInput, "B");
        }
    }

    private class MyItalicAction extends BaseAction {

        private MyItalicAction(Controller controller) {
            super("action_italic", controller);
        }

        public void actionPerformed(ActionEvent e) {
            toggle(chatInput, "I");
        }
    }

    private class MyUnderlineAction extends BaseAction {

        private MyUnderlineAction(Controller controller) {
            super("action_underline", controller);
        }

        public void actionPerformed(ActionEvent e) {
            toggle(chatInput, "U");
        }
    }

    private static class DocumentSection {

        private String type;
        private String text;

        private DocumentSection(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }

}
