package de.dal33t.powerfolder.ui.chat;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.MemberChatMessage;
import de.dal33t.powerfolder.ui.action.ChangeFriendStatusAction;
import de.dal33t.powerfolder.ui.friends.NodeQuickInfoPanel;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIPanel;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Chat with a member.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 * @see ChatPanel
 */
public class MemberChatPanel extends ChatPanel implements UIPanel {
    private NodeQuickInfoPanel quickInfoPanel;
    private ChangeFriendStatusAction changeFriendStatusAction;
    /** The blink manager that takes care of blinking icons in the tree for chat. */
    private BlinkManager treeBlinkManager;
    /**
     * holds the member we chat with (only if chatting with a member) for the
     * changeFriendStatusAction
     */
    private SelectionModel memberSelectionModel;

    /**
     * create a chatpanel
     * 
     * @param controller
     */
    public MemberChatPanel(Controller controller) {
        super(controller);
        treeBlinkManager = controller.getUIController().getBlinkManager();
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        memberSelectionModel = new SelectionModel();
        changeFriendStatusAction = new ChangeFriendStatusAction(controller,
            memberSelectionModel);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            // main layout
            FormLayout layout = new FormLayout("fill:0:grow",
                "pref, pref, 3dlu, pref, fill:0:grow, pref, 3dlu, pref, pref, pref, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(quickInfoPanel.getUIComponent(), cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.addSeparator(null, cc.xy(1, 4));
            
            builder.add(scrollPaneOutput, cc.xy(1, 5));
            builder.addSeparator(null, cc.xy(1, 6));
            builder.addSeparator(null, cc.xy(1, 8));
            builder.add(scrollPaneInput, cc.xy(1, 9));
            builder.addSeparator(null, cc.xy(1, 10));
            builder.add(toolBar, cc.xy(1, 11));
            panel = builder.getPanel();
        }
        return panel;
    }

    void initComponents() {
        super.initComponents();
        quickInfoPanel = new NodeQuickInfoPanel(getController());
        toolBar = createToolBar();
        getUIController().getChatModel().addChatModelListener(
            new TheChatModelListener());
        chatInput.addKeyListener(new ChatKeyListener());
    }

    private JPanel createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addRelatedGap();
        bar.addFixed(new JButton(changeFriendStatusAction));
        bar.setBorder(Borders.DLU4_BORDER);
        return bar.getPanel();
    }

    /**
     * @return the title of the active chat
     */
    public String getTitle() {
        if (getChatPartner() != null) {
            return Translation.getTranslation("chatpanel.chat_with") + " "
                + getChatPartner().getNick();
        }
        return null;
    }

    /**
     * @param member
     *            the Member to chat about
     */
    public void setChatPartner(Member member) {
        if (treeBlinkManager.isBlinking(member)) {
            treeBlinkManager.removeBlinkMember(member);
        }
        memberSelectionModel.setSelection(member);
        updateChat();

    }

    /**
     * @return the member we are currently chatting with.
     */
    public Member getChatPartner() {
        return (Member) memberSelectionModel.getSelection();
    }

    private void updateChat() {
        ChatModel.ChatLine[] lines = null;
        if (getChatPartner() != null) {
            lines = getUIController().getChatModel().getChatText(getChatPartner());
            if (lines != null) {
                updateChat(lines);
            }
        }
    }

    /**
     * Updates the input field state (enabled/disabled) accoriding the the chat
     * partner
     */
    public void updateInputField() {
        if (getChatPartner() != null) {
            enableInputField(getChatPartner().isCompleteyConnected());
        } else {
            enableInputField(true);
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
            if (source instanceof Member) {
                Member member = (Member) source;
                if (getChatPartner() != null && getChatPartner().equals(source)) {
                    // only update if the source is the current chat
                    updateChat();
                }

                // FIXME Move this into nodemanager model
                if (!event.isStatus()) {
                    if (!getUIController().getNodeManagerModel().hasMemberNode(
                        member))
                    {
                        getUIController().getNodeManagerModel().addChatMember(
                            member);
                    }
                }
            }
        }

        public boolean fireInEventDispathThread() {
            return true;
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
                Member withMember = getChatPartner();
                if (message.trim().length() > 0) { // no SPAM on "enter"
                    if (getChatPartner() != null) {
                        if (withMember.isCompleteyConnected()) {
                            getUIController().getChatModel().addChatLine(
                                withMember, getController().getMySelf(),
                                message);
                            chatInput.setText("");
                            MemberChatMessage mcMessage = new MemberChatMessage(
                                message);

                            withMember.sendMessageAsynchron(mcMessage,
                                "chatline not send");
                        } else {
                            getUIController().getChatModel().addStatusChatLine(
                                withMember,
                                Translation.getTranslation(
                                    "chatpanel.cannot_deliver", withMember
                                        .getNick()));
                        }
                    }
                } else {// enter key without text
                    chatInput.setText("");
                    chatInput.requestFocusInWindow();
                }
                // Update input field
                updateInputField();
            }
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
            if (e.getNode().equals(getChatPartner())) {
                updateInputField();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (e.getNode().equals(getChatPartner())) {
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

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
