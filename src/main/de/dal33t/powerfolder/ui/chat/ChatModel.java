package de.dal33t.powerfolder.ui.chat;

import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FolderChatMessage;
import de.dal33t.powerfolder.message.MemberChatMessage;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;

/**
 * Holds all Chat data. A ChatModel.ChatBox per Folder and Member. A
 * ChatModel.ChatBox is a collection of ChatModel.ChatLine s or StatusChatLine
 * s. A ChatLine has a fromMember and a text. StatusChatLine is a wrapper class
 * with no extra content it just is an indication that it's a message like "xyz
 * connected at [11:06:50]"
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class ChatModel implements MessageListener {
    /** key = Folder, value =ChatBox */
    private HashMap<Folder, ChatBox> folderChatBoxMap = new HashMap<Folder, ChatBox>();
    /** key = Member, value = ChatBox */
    private HashMap<Member, ChatBox> memberChatBoxMap = new HashMap<Member, ChatBox>();
    /** All listners to this Model */
    private List<ChatModelListener> chatModelListeners = new LinkedList<ChatModelListener>();

    private Controller controller;
    private FolderRepository repository;
    private FolderMembershipListener folderMembershipListener;

    public ChatModel(Controller controller) {
        this.controller = controller;
        this.repository = controller.getFolderRepository();
        // register to receive messages
        NodeManager nodeManager = controller.getNodeManager();
        nodeManager.addMessageListenerToAllNodes(this);
        // receive connected/disconnected events
        nodeManager.addNodeManagerListener(new NodeListener());
        // this listeners will be added to the folders
        folderMembershipListener = new MembershipFoldersListener();
        addListenerToExsistingFolders();
        repository.addFolderRepositoryListener(new RepositoryListener());
    }

    /**
     * Adds a message to a ChatBox about a Folder from a member
     * 
     * @param folder
     *            The Folder to select the correct ChatBox
     * @param member
     *            The Member this message comes from
     * @param message
     *            The actual line of text
     */
    public void addChatLine(Folder folder, Member member, String message) {
        ChatBox chat = getChatBox(folder);
        chat.addLine(new ChatLine(member, message));
        fireChatModelChanged(folder, message, false);
    }

    public void addStatusChatLine(Folder folder, Member about, String message) {
        ChatBox chat = getChatBox(folder);
        chat.addLine(new StatusChatLine(about, message));
        fireChatModelChanged(folder, message, true);
    }

    /**
     * Adds a message to a ChatBox about with member from a member
     * 
     * @param other
     *            The other Member that is chatted with
     * @param typedMember
     *            The member that typed the message (myself or the other member)
     * @param The
     *            actual line of text
     */
    public void addChatLine(Member other, Member typedMember, String message) {
        ChatBox chat = getChatBox(other);
        chat.addLine(new ChatLine(typedMember, message));
        fireChatModelChanged(other, message, false);
    }

    public void addStatusChatLine(Member about, String message) {
        ChatBox chat = getChatBox(about);
        chat.addLine(new StatusChatLine(about, message));
        fireChatModelChanged(about, message, true);
    }

    /**
     * Returns true if there is a chat session with the given member.
     * 
     * @param other
     *            the other member that is chatted with
     * @return true if there is a chat
     */
    public boolean hasChatBoxWith(Member other) {
        return memberChatBoxMap.containsKey(other);
    }

    /**
     * Get all chatlines from a chat about a Folder
     * 
     * @param folder
     *            Which folder to get the chat lines for
     * @return A array of all chat lines, first item is the oldest chat line.
     */
    public ChatLine[] getChatText(Folder folder) {
        ChatBox chat = getChatBox(folder);
        return chat.getChatText();
    }

    /**
     * Get all chatlines from a chat with a member
     * 
     * @param meber
     *            Which member to get the chat lines for
     * @return A array of all chat lines, first item is the oldest chat line.
     */
    public ChatLine[] getChatText(Member member) {
        ChatBox chat = getChatBox(member);
        return chat.getChatText();
    }

    /**
     * returns the ChatBox for this Member
     */
    private ChatBox getChatBox(Member member) {
        ChatBox chat;
        if (memberChatBoxMap.containsKey(member)) {
            chat = memberChatBoxMap.get(member);
        } else {
            chat = new ChatBox();
            memberChatBoxMap.put(member, chat);
        }
        return chat;
    }

    /**
     * retruns the ChatBox for this folder
     * 
     * @param folder
     * @return The ChatBox associated with this folder
     */
    private ChatBox getChatBox(Folder folder) {
        ChatBox chat;
        if (folderChatBoxMap.containsKey(folder)) {
            chat = folderChatBoxMap.get(folder);
        } else {
            chat = new ChatBox();
            folderChatBoxMap.put(folder, chat);
        }
        return chat;
    }

    /**
     * called if a message is received from a remote Member this can be any type
     * of message we only handle chat messages.
     */
    public void handleMessage(Member source, Message message) {
        if (message instanceof FolderChatMessage) {
            FolderChatMessage fcMessage = (FolderChatMessage) message;
            FolderInfo folderInfo = fcMessage.folder;
            Folder folder = controller.getFolderRepository().getFolder(
                folderInfo);
            addChatLine(folder, source, fcMessage.text);
        } else if (message instanceof MemberChatMessage) {
            MemberChatMessage mcMessage = (MemberChatMessage) message;
            addChatLine(source, source, mcMessage.text);
        }
    }

    /** add a listener that will recieve events if chatlines are recieved */
    public void addChatModelListener(ChatModelListener cmListener) {
        chatModelListeners.add(cmListener);
    }

    /**
     *  calls all listeners 
     * @param source
     * @param message
     * @param isStatus
     */
    private void fireChatModelChanged(Object source, String message, boolean isStatus) {
        for (int i = 0; i < chatModelListeners.size(); i++) {
            ChatModelListener chatModelListener = chatModelListeners.get(i);
            chatModelListener.chatChanged(new ChatModelEvent(source, message, isStatus));
        }
    }

    /** a Chatline is a text and the member who typed it */
    public class ChatLine {
        /** typed this text */
        Member fromMember;
        /** the text */
        String text;

        /** create a new chatline */
        ChatLine(Member fromMember, String text) {
            this.fromMember = fromMember;
            this.text = text;
        }
    }

    public class StatusChatLine extends ChatLine {
        /** create a new status chatline */
        StatusChatLine(Member fromMember, String text) {
            super(fromMember, text);
        }
    }

    /** A chatBox is a collection of ChatLines */
    private class ChatBox {
        List<ChatLine> chatLines = new LinkedList<ChatLine>();

        void addLine(ChatLine line) {
            chatLines.add(line);
            if (chatLines.size() > Constants.MAX_CHAT_LINES) {
                chatLines.remove(0);
            }
        }

        ChatLine[] getChatText() {
            Object[] lines = chatLines.toArray();
            ChatLine[] linesReturnValue = new ChatLine[chatLines.size()];
            System.arraycopy(lines, 0, linesReturnValue, 0, chatLines.size());
            return linesReturnValue;
        }
    }

    /**
     * interface to implement to Listen to chat events from the ChatModel
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     */
    public interface ChatModelListener {
        public void chatChanged(ChatModelEvent event);
    }

    /**
     * event that holds a Folder or Member that on which the chat text has
     * changed
     * <p>
     * TODO Split source up into sourceFolder and sourceMember. A Folder chat
     * message update has both!
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     */
    public class ChatModelEvent extends EventObject {
        private static final long serialVersionUID = 1L;
        private boolean isStatus;
        private String message = StringUtils.EMPTY;

        // source is the folder or Member
        public ChatModelEvent(Object source) {
            super(source);
            this.isStatus = false;
        }

        public ChatModelEvent(Object source, boolean flag) {
            super(source);
            this.isStatus = flag;
        }

        public ChatModelEvent(Object source, String message, boolean flag) {
            super(source);
            this.message = message;
            this.isStatus = flag;
        }
        
        public boolean isStatus() {
            return isStatus;
        }
        
        public String getMessage() {
            return message;
        }
    }

    // adds the listener to all previously created folders
    private void addListenerToExsistingFolders() {
        Folder[] folders = repository.getFolders();
        for (int i = 0; i < folders.length; i++) {
            folders[i].addMembershipListener(folderMembershipListener);
        }
    }

    // our listner to all folder creation/removing event, adds and removes our
    // listener if one is created of removed.
    private class RepositoryListener implements FolderRepositoryListener {

        public void folderCreated(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            folder.addMembershipListener(folderMembershipListener);
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            folder.removeMembershipListener(folderMembershipListener);
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * listen to the folder join/left events from the folders and add status
     * messages in the chat
     */
    private class MembershipFoldersListener implements FolderMembershipListener
    {
        // a member joined a folder
        public void memberJoined(FolderMembershipEvent folderEvent) {
            Member node = folderEvent.getMember();
            Folder folder = (Folder) folderEvent.getSource();
            String statusMessage = Translation.getTranslation(
                "chatpanel.member_joined_folder_at_time", node.getNick(),
                Format.getTimeOnlyDateFormat().format(new Date()) + "")
                + "\n";
            addStatusChatLine(folder, node, statusMessage);
        }

        // a member left a folder
        public void memberLeft(FolderMembershipEvent folderEvent) {
            Member node = folderEvent.getMember();
            Folder folder = (Folder) folderEvent.getSource();
            String statusMessage = Translation.getTranslation(
                "chatpanel.member_left_folder_at_time", node.getNick(),
                Format.getTimeOnlyDateFormat().format(new Date()) + "")
                + "\n";
            addStatusChatLine(folder, node, statusMessage);
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

    }

    /**
     * listen to the connected/disconnected events from nodes and add status
     * messages in the chat
     */
    private class NodeListener implements NodeManagerListener {

        public void nodeConnected(NodeManagerEvent e) {
            Member node = e.getNode();
            String statusMessage = Translation.getTranslation(
                "chatpanel.member_connected_at_time", node.getNick(),
                Format.getTimeOnlyDateFormat().format(new Date()) + "")
                + "\n";
            addStatusChatLine(node, statusMessage);
            List folders = repository.getFoldersAsSortedList();
            for (int i = 0; i < folders.size(); i++) {
                Folder folder = (Folder) folders.get(i);
                if (folder.hasMember(node)) {
                    addStatusChatLine(folder, node, statusMessage);
                }
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            Member node = e.getNode();
            String statusMessage = Translation.getTranslation(
                "chatpanel.member_disconnected_at_time", node.getNick(),
                Format.getTimeOnlyDateFormat().format(new Date()) + "")
                + "\n";
            addStatusChatLine(node, statusMessage);
            List folders = repository.getFoldersAsSortedList();
            for (int i = 0; i < folders.size(); i++) {
                Folder folder = (Folder) folders.get(i);
                if (folder.hasMember(node)) {
                    addStatusChatLine(folder, node, statusMessage);
                }
            }
        }

        // ignore
        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }
        
        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
