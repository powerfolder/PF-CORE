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
package de.dal33t.powerfolder.ui.chat;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A simple chat functionality. Base class. TODO: make styled messages
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom </A>
 * @version $Revision: 1.2 $
 */
public abstract class ChatPanel extends PFUIPanel {

    /** The Input field where the user can enter chatlines */
    JTextArea chatInput;
    /** The output field where all chatlines are displayed */
    JTextPane chatOutput;

    /** The scrollPanle holding the chatOutput. */
    JScrollPane scrollPaneOutput;
    /** The scrollPanle holding the chatinput. */
    JScrollPane scrollPaneInput;

    JPanel panel;
    JPanel toolBar;

    // styles used in StyledDocument
    private static final String NORMAL = "normal";
    private static final String BOLD = "bold";
    private static final String BOLD_BLUE = "bold-blue";
    private static final String BOLD_GREEN = "bold-green";

    /** create a chatpanel 
     * @param controller */
    public ChatPanel(Controller controller) {
        super(controller);
    }

    void initComponents() {
        chatInput = new JTextArea(5, 30);
        chatInput.setEditable(true);
        chatOutput = new JTextPane();
        chatOutput.setEditable(false);

        StyledDocument doc = chatOutput.getStyledDocument();

        try {
            doc.insertString(0, "", null);
        } catch (BadLocationException ble) {
            log().warn("Couldn't insert initial text into text pane.");
        }
        scrollPaneInput = new JScrollPane(chatInput,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        UIUtil.removeBorder(scrollPaneInput);

        scrollPaneOutput = new JScrollPane(chatOutput,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chatOutput.getParent().setBackground(Color.WHITE);
        UIUtil.removeBorder(scrollPaneOutput);

    }

    private void createStyles(StyledDocument doc) {
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
     * called if a chatline is recieved, completely replaces the text in the
     * output field.
     */
    void updateChat(ChatModel.ChatLine[] lines) {
        final StyledDocument doc = new DefaultStyledDocument();
        createStyles(doc);
        try {
            for (int i = 0; i < lines.length; i++) {
                ChatModel.ChatLine line = lines[i];
                if (line instanceof ChatModel.StatusChatLine) {
                    doc.insertString(doc.getLength(), "* " + line.text, doc
                        .getStyle(BOLD_GREEN));

                } else {
                    Member otherMember = line.fromMember;
                    String text = line.text;
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
            log().warn("could not set chat text", ble);
        }

        Runnable runner = new Runnable() {
            public void run() {
                chatOutput.setStyledDocument(doc);
                // hack to make sure first text is at bottom
                while (chatOutput.getPreferredSize().height < chatOutput
                    .getSize().height)
                {
                    try {
                        doc.insertString(0, "\n", null);
                    } catch (BadLocationException ble) {
                        log().warn("could not set chat text", ble);
                    }
                }

                Rectangle r = new Rectangle(1,
                    chatOutput.getPreferredSize().height, 1, 1);
                // make sure the last line of text is visible
                scrollPaneOutput.getViewport().scrollRectToVisible(r);
                // make sure we can always type text in the input
                chatInput.requestFocusInWindow();

                ChatPanel.this.updateInputField();
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * called after a the chat updated, maybe disable the input depending on the
     * online status.
     */
    abstract void updateInputField();

    void enableInputField(boolean enable) {
        if (chatInput != null) {
            if (enable) {
                chatInput.setBackground(Color.WHITE);
            } else {
                chatInput.setBackground(Color.LIGHT_GRAY);
            }
            chatInput.setEnabled(enable);
        }
    }

}