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
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.action.ChangeFriendStatusAction;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Chat with a member.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 * @see ChatPanel
 */
public class MemberChatPanel extends ChatPanel {
    private ChangeFriendStatusAction changeFriendStatusAction;
    /** The blink manager that takes care of blinking icons in the tree for chat. */
    private BlinkManager treeBlinkManager;
    /** The Member to chat with */
    private Member withMember;
    /**
     * holds the member we chat with (only if chatting with a member) for the
     * changeFriendStatusAction
     */
    private SelectionModel memberSelectionModel;

    /** create a chatpanel */
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
                "fill:0:grow, pref, 3dlu, pref, pref, pref, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(scrollPaneOutput, cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.addSeparator(null, cc.xy(1, 4));
            builder.add(scrollPaneInput, cc.xy(1, 5));
            builder.addSeparator(null, cc.xy(1, 6));
            builder.add(toolBar, cc.xy(1, 7));
            panel = builder.getPanel();
        }
        return panel;
    }

    void initComponents() {
        super.initComponents();
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
     * Returns the title of the active chat
     * 
     * @return
     */
    public String getTitle() {
        if (withMember != null) {
            return Translation.getTranslation("chatpanel.chat_with") + " "
                + withMember.getNick();
        }
        return null;
    }

    /** Set the Folder to chat about */
    public void chatWithMember(Member member) {
        if (treeBlinkManager.isBlinkingMember(member)) {
            treeBlinkManager.removeBlinkMember(member);
        }
        withMember = member;
        memberSelectionModel.setSelection(member);
        updateChat();

    }

    private void updateChat() {
        ChatModel.ChatLine[] lines = null;
        if (withMember != null) {
            lines = getUIController().getChatModel().getChatText(withMember);
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
        if (withMember != null) {
            enableInputField(withMember.isCompleteyConnected());
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
                if (withMember != null && withMember.equals(member)) {
                    // only update if the source is the current chat
                    updateChat();
                }

                UIController uiController = getUIController();
                // if not the current chat is the chat with the member we
                // recieved a message from, we will BLINK
                // and the message is not a status message

                if (!event.isStatus()) {
                    if (!getController().getNodeManager().hasMemberNode(member))
                        getController().getNodeManager().addChatMember(member);

                    if (withMember == null
                        || !withMember.equals(member)
                        || !(uiController.getInformationQuarter()
                            .getDisplayTarget() instanceof ChatPanel)
                        || !panel.isVisible())
                    {
                        if (!member.isMySelf()) {
                            treeBlinkManager.addBlinkMember(member, Icons.CHAT);
                        }
                    }
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
                    if (withMember != null) {
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
            if (e.getNode().equals(withMember)) {
                updateInputField();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (e.getNode().equals(withMember)) {
                updateInputField();
            }
        }

        public void friendAdded(NodeManagerEvent e) {
            if (e.getNode().equals(withMember)) {
                TreeNodeList chatNodes = getController().getNodeManager()
                    .getChatTreeNodes();
                chatNodes.removeChild(withMember);
                updateTreeNode();
            }
        }

        private void updateTreeNode() {
            getUIController().getControlQuarter().getNavigationTreeModel()
                .fireChatNodeUpdatedAndExpand();
        }

        public void friendRemoved(NodeManagerEvent e) {
            if (e.getNode().equals(withMember)) {
                TreeNodeList chatNodes = getController().getNodeManager()
                    .getChatTreeNodes();
                chatNodes.addChild(withMember);
                updateTreeNode();
            }
        }

        public void settingsChanged(NodeManagerEvent e) {
        }
    }
}
