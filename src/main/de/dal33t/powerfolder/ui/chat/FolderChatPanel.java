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
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Chats about Folder. Chat messages about Folders are send to all members of a
 * Folder.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 * @see ChatPanel
 */
public class FolderChatPanel extends ChatPanel {
    private final ChatModel chatModel;
    /** The Folder To Chat about */
    private Folder aboutFolder;

    /**
     * create a chatpanel
     * 
     * @param controller
     *            the controller.
     * @param model
     *            the chatmodel to use.
     */
    public FolderChatPanel(Controller controller, ChatModel model) {
        super(controller);
        Reject.ifNull(model, "Model is null");
        chatModel = model;
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
        chatModel.addChatModelListener(new TheChatModelListener());
        chatInput.addKeyListener(new ChatKeyListener());
    }

    /**
     * @return the title of the active chat
     */
    public String getTitle() {
        if (aboutFolder != null) {
            return Translation.getTranslation("chatpanel.chat_about") + " "
                + aboutFolder.getName();
        }
        return null;
    }

    /**
     * Set the Folder to chat about.
     * 
     * @param folder
     *            the folder to display the chat for.
     */
    public void setChatFolder(Folder folder) {
        if (getUIController().getBlinkManager().isBlinking(folder)) {
            getUIController().getBlinkManager().removeBlinking(folder);
        }
        aboutFolder = folder;
        updateChat();
    }

    /**
     * @return the folder currently chatting on
     */
    public Folder getChatFolder() {
        return aboutFolder;
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
            lines = chatModel.getChatText(aboutFolder);
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
            if (aboutFolder != null && aboutFolder.equals(source)) {
                // only update if the source is the current chat
                updateChat();
            }
        }
    }

    /** check for enter key in the input field and sends the message. */
    private class ChatKeyListener extends KeyAdapter {
        public void keyTyped(KeyEvent e) {
            char keyTyped = e.getKeyChar();
            if (keyTyped == '\n') { // enter key = send message
                String message = chatInput.getText();
                if (message.trim().length() > 0) { // no SPAM on "enter"
                    if (aboutFolder != null) {
                        chatModel.addChatLine(aboutFolder, getController()
                            .getMySelf(), message);
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
                }
            }
        }
    }
}
