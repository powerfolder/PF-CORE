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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.message.MemberChatMessage;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Holds all Chat data. A ChatBox per Member.
 * A ChatBox is a collection of ChatLine s.
 * A ChatLine has a fromMember and a text.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class ChatModel {

    private Map<Member, ChatBox> chatBoxMap = new HashMap<Member, ChatBox>();
    private ChatModelListener chatModelListeners;
    private Controller controller;
    private FolderRepository repository;
    private FolderMembershipListener folderMembershipListener;

    /**
     * Constructor
     *
     * @param controller
     */
    public ChatModel(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        this.controller = controller;
        repository = controller.getFolderRepository();
        NodeManager nodeManager = controller.getNodeManager();
        MessageListener messageListener = new MyMessageListener();
        nodeManager.addMessageListenerToAllNodes(messageListener);
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
        folderMembershipListener = new MyFolderMembershipListener();
        addListenerToExsistingFolders();
        repository.addFolderRepositoryListener(new MyFolderRepositoryListener());
        chatModelListeners = ListenerSupportFactory.createListenerSupport(
                ChatModelListener.class);

    }

    /**
     * Adds a message to a ChatBox about with member from a member
     *
     * @param toMember The other Member that is chatted with
     * @param fromMember The member that typed the message (myself or the other member)
     * @param message The actual line of text
     */
    public void addChatLine(Member toMember, Member fromMember, String message) {
        ChatBox chat = getChatBox(toMember);
        ChatLine chatLine = new ChatLine(fromMember, message);
        chat.addLine(chatLine);
        fireChatModelChanged(toMember, chatLine);
    }

    /**
     * Adds a status line about a member.
     *
     * @param member
     * @param message
     */
    public void addStatusChatLine(Member member, String message) {
        ChatBox chat = getChatBox(member);
        ChatLine chatLine = new ChatLine(member, message, true);
        chat.addLine(chatLine);
        fireChatModelChanged(member, chatLine);
    }

    /**
     * Returns true if there is a chat session with the given member.
     *
     * @param other the other member that is chatted with
     * @return true if there is a chat
     */
    public boolean hasChatBoxWith(Member other) {
        return chatBoxMap.containsKey(other);
    }

    /**
     * Get all chatlines from a chat with a member
     *
     * @param member Which member to get the chat lines for
     * @return A array of all chat lines, first item is the oldest chat line.
     */
    public ChatLine[] getChatText(Member member) {
        ChatBox chat = getChatBox(member);
        return chat.getChatText();
    }

    /**
     * Returns the ChatBox for this Member
     */
    private ChatBox getChatBox(Member member) {
        ChatBox chat;
        if (chatBoxMap.containsKey(member)) {
            chat = chatBoxMap.get(member);
        } else {
            chat = new ChatBox();
            chatBoxMap.put(member, chat);
        }
        return chat;
    }

    /**
     * Add a listener that will receive events if chatlines are received
     *
     * @param cmListener
     */
    public void addChatModelListener(ChatModelListener cmListener) {
        ListenerSupportFactory.addListener(chatModelListeners, cmListener);
    }

    /**
     * Calls all listeners
     *
     * @param source
     * @param line
     */
    private void fireChatModelChanged(Object source, ChatLine line) {
        chatModelListeners.chatChanged(new ChatModelEvent(source, line.getText(),
                line.isStatus()));
    }

    /**
     * Adds the listener to all previously created folders
     */
    private void addListenerToExsistingFolders() {
        Collection<Folder> folders = repository.getFolders();
        for (Folder folder : folders) {
            folder.addMembershipListener(folderMembershipListener);
        }
    }

    // Our listner to all folder creation/removing event, adds and removes our
    // listener if one is created of removed.
    private class MyFolderRepositoryListener implements
            FolderRepositoryListener {

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

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    /**
     * Listen to the folder join/left events from the folders and add status
     * messages in the chat
     */
    private class MyFolderMembershipListener implements
            FolderMembershipListener {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            Member node = folderEvent.getMember();
            String statusMessage = Translation.getTranslation(
                    "chat_panel.member_joined_folder_at_time", node.getNick(),
                    folderEvent.getFolder().getName(),
                    Format.getTimeOnlyDateFormat().format(new Date()))
                    + '\n';
            addStatusChatLine(node, statusMessage);
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            Member node = folderEvent.getMember();
            String statusMessage = Translation.getTranslation(
                    "chat_panel.member_left_folder_at_time", node.getNick(),
                    folderEvent.getFolder().getName(),
                    Format.getTimeOnlyDateFormat().format(new Date())) + '\n';
            addStatusChatLine(node, statusMessage);
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    /**
     * Listen to the connected/disconnected events from nodes and add status
     * messages in the chat
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public void nodeConnected(NodeManagerEvent e) {
            if (controller.isVerbose()) {
                Member node = e.getNode();
                String statusMessage = Translation.getTranslation(
                    "chat_panel.member_connected_at_time", node.getNick(),
                    Format.getTimeOnlyDateFormat().format(new Date())) + '\n';
                addStatusChatLine(node, statusMessage);
                for (Folder folder : repository.getFolders()) {
                    if (folder.hasMember(node)) {
                        addStatusChatLine(node, statusMessage);
                    }
                }
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (controller.isVerbose()) {
                Member node = e.getNode();
                String statusMessage = Translation.getTranslation(
                    "chat_panel.member_disconnected_at_time", node.getNick(),
                    Format.getTimeOnlyDateFormat().format(new Date())) + '\n';
                addStatusChatLine(node, statusMessage);
                for (Folder folder : repository.getFolders()) {
                    if (folder.hasMember(node)) {
                        addStatusChatLine(node, statusMessage);
                    }
                }
            }
        }

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

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    private class MyMessageListener implements MessageListener {

        /**
         * Called if a message is received from a remote Member.
         * This can be any type of message.
         * We only handle chat messages.
         */
        public void handleMessage(Member source, Message message) {
            if (message instanceof MemberChatMessage) {
                MemberChatMessage mcMessage = (MemberChatMessage) message;
                addChatLine(source, source, mcMessage.text);
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}