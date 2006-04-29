package de.dal33t.powerfolder.ui.chat;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.message.FolderChatMessage;
import de.dal33t.powerfolder.util.Translation;

/**
 * Chats about Folder. Chat messages about Folders are send to all members of a
 * Folder.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 * @see ChatPanel
 */
public class FolderChatPanel extends ChatPanel {
    /** The Folder To Chat about */
    private Folder aboutFolder;

    /** create a chatpanel */
    public FolderChatPanel(Controller controller) {
        super(controller);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();

            // main layout
            FormLayout layout = new FormLayout("fill:0:grow",
                "fill:0:grow, pref, 3dlu, pref, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(scrollPaneOutput, cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.addSeparator(null, cc.xy(1, 4));
            builder.add(scrollPaneInput, cc.xy(1, 5));
            panel = builder.getPanel();
        }
        return panel;
    }

    void initComponents() {
        super.initComponents();
        getController().getChatModel().addChatModelListener(
            new TheChatModelListener());
        chatInput.addKeyListener(new ChatKeyListener());
    }

    /**
     * Returns the title of the active chat
     * 
     * @return
     */
    public String getTitle() {
        if (aboutFolder != null) {
            return Translation.getTranslation("chatpanel.chat_about") + " "
                + aboutFolder.getName();
        }
        return null;
    }

    /** Set the Folder to chat about */
    public void chatAbout(Folder folder) {
        aboutFolder = folder;
        updateChat();
    }

    /**
     * Updates the input field state (enabled/disabled)
     */
    public void updateInputField() {
        if (aboutFolder != null) {
            enableInputField(true);
        }
    }

    private void updateChat() {
        ChatModel.ChatLine[] lines = null;
        if (aboutFolder != null) {
            lines = getController().getChatModel().getChatText(aboutFolder);
            if (lines != null) {
                updateChat(lines);
            }
        }
    }

    /** updates the chat if the current chat is changed */
    private class TheChatModelListener implements ChatModel.ChatModelListener {
        /**
         * called from the model if a message (about a Folder or Member) is
         * received from other member or typed by this member (myself)
         */
        public void chatChanged(ChatModel.ChatModelEvent event) {
            Object source = event.getSource();

            if (aboutFolder != null) {
                if (aboutFolder.equals(source)) {
                    // only update if the source is the current chat
                    updateChat();
                }
            }

        }
    }

    /** check for enter key in the input field and sends the message. */
    private class ChatKeyListener extends KeyAdapter {
        public void keyTyped(KeyEvent e) {
            char keyTyped = e.getKeyChar();
            if (keyTyped == '\n') { // enter key = send message
                // disable the input
                enableInputField(false);
                String message = chatInput.getText();
                if (message.trim().length() > 0) { // no SPAM on "enter"
                    if (aboutFolder != null) {
                        getController().getChatModel().addChatLine(aboutFolder,
                            getController().getMySelf(), message);
                        chatInput.setText("");
                        // create a message
                        FolderChatMessage fcMessage = new FolderChatMessage(
                            aboutFolder.getInfo(), message);
                        // send it to members of this folder
                        aboutFolder.broadcastMessage(fcMessage);
                    } else {// enter key without text
                        chatInput.setText("");
                        chatInput.requestFocusInWindow();
                    }

                    enableInputField(true);
                }
            }
        }
    }

}
