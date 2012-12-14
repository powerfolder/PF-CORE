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
package de.dal33t.powerfolder.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.SendInvitationEmail;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.task.SendMessageTask;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;

/**
 * methods for loading and saving powerfolder invitations
 * 
 * @see Invitation
 */
public class InvitationUtil {

    private static final Logger log = Logger.getLogger(InvitationUtil.class
        .getName());

    // No instances
    private InvitationUtil() {
    }

    public static boolean isDefaultMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.equalsIgnoreCase("Attach a personal message")
            || message.equalsIgnoreCase("Persönliche Nachrichten anhängen");
    }

    /**
     * Loads an invitation from a file. Return the invitation or null if not
     * possible to load the file
     * 
     * @param file
     *            The file to load the invitation from
     * @return the invitation, null if file not found or on other error.
     */
    public static Invitation load(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            return null;
        }
        log.fine("Loading invitation " + file);
        try {
            FileInputStream fIn = new FileInputStream(file);
            return load(fIn);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to read invitation file stream", e);
        }
        return null;
    }

    /**
     * Loads an invitation from a file. Return the invitation or null if not
     * possible to load the file
     * 
     * @param in
     *            the stream to read from
     * @return the Invitation
     */
    public static Invitation load(InputStream in) {
        if (in == null) {
            throw new NullPointerException("File is null");
        }
        log.fine("Loading invitation from " + in);
        try {
            ObjectInputStream oIn = new ObjectInputStream(in);
            Invitation invitation = (Invitation) oIn.readObject();

            if (invitation.getInvitor() == null) {
                // Old file version, has another member info at end
                // New invitation files have memberinfo inclueded in invitation
                try {
                    MemberInfo from = (MemberInfo) oIn.readObject();
                    if (invitation.getInvitor() == null) {
                        // Use invitation
                        invitation.setInvitor(from);
                    }
                } catch (IOException e) {
                    // Ingnore
                }
            }

            in.close();

            return invitation;
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, "Unable to read invitation file stream", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to read invitation file stream", e);
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Unable to read invitation file stream", e);
        }
        return null;
    }

    /**
     * Save an Invitation to a File
     * 
     * @param invitation
     *            the invitation to save
     * @param file
     *            the file to save to
     * @return true if succeeded
     */
    public static boolean save(Invitation invitation, File file) {
        try {
            return save(invitation, new BufferedOutputStream(
                new FileOutputStream(file)));
        } catch (FileNotFoundException e) {
            log.log(Level.SEVERE, "Unable to write invitation file stream", e);
            return false;
        }
    }

    /**
     * Save an Invitation to an Outputstream.
     * 
     * @param invitation
     *            the invitation to save
     * @param out
     *            the stream to save to
     * @return true if successful
     */
    public static boolean save(Invitation invitation, OutputStream out) {
        log.finer("Saving invitation to " + out);
        ObjectOutputStream oOut;
        try {
            oOut = new ObjectOutputStream(out);
            oOut.writeObject(invitation);
            oOut.close();
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to save invitation file stream", e);
        }
        return false;
    }

    /**
     * Creates a file filter for powerfolder invitations
     * 
     * @return a filter accepting .invitation files only.
     */
    public static FileFilter createInvitationsFilefilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".invitation") || f.isDirectory();
            }

            public String getDescription() {
                return Translation
                    .getTranslation("invitation_files.description");
            }
        };
    }

    /**
     * Handles the invitation to mail option
     * 
     * @param controller
     *            the controller
     * @param invitation
     *            the invitation
     * @param to
     *            the destination email address, if null the user is asked for.
     */
    public static void invitationByServer(Controller controller,
        Invitation invitation, String to, boolean ccMe)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(invitation, "Invitation is null");

        controller.getOSClient().getFolderService().sendInvitationEmail(
                new SendInvitationEmail(invitation, to, ccMe));
    }

    /**
     * Handles the invitation to disk option.
     * 
     * @param controller
     *            the controller
     * @param invitation
     *            the invitation
     * @param file
     *            the file to write to, if null the users is asked for.
     * @return if the file was written.
     */
//    public static boolean invitationToDisk(Controller controller,
//        Invitation invitation, File file)
//    {
//        Reject.ifNull(controller, "Controller is null");
//        Reject.ifNull(invitation, "Invitation is null");
//
//        // Select file
//        if (file == null) {
//            JFileChooser fc = DialogFactory.createFileChooser();
//            fc.setDialogTitle(Translation
//                .getTranslation("send_invitation.placetostore"));
//            // Recommended file
//            fc.setSelectedFile(new File(invitation.folder.name + ".invitation"));
//            fc.setFileFilter(createInvitationsFilefilter());
//            int result = fc.showSaveDialog(controller.getUIController()
//                .getMainFrame().getUIComponent());
//            if (result != JFileChooser.APPROVE_OPTION) {
//                return false;
//            }
//
//            // Store invitation to disk
//            file = fc.getSelectedFile();
//            if (file == null) {
//                return false;
//            }
//            if (file.exists()) {
//                // TODO: Add confirm dialog
//            }
//        }
//
//        log.info("Writing invitation to " + file);
//        if (!save(invitation, file)) {
//            DialogFactory.genericDialog(controller, Translation
//                .getTranslation("invitation.utils.unable.write.title"),
//                Translation
//                    .getTranslation("invitation.utils.unable.write.text"),
//                GenericDialogType.ERROR);
//            return false;
//        }
//
//        return true;
//    }

    /**
     * Sends an invitation to a connected node.
     * 
     * @param controller
     * @param invitation
     * @param node
     * @return true if invitation could be sent. false if the invitation was
     *         scheduled for later sending.
     */
    public static boolean invitationToNode(Controller controller,
        Invitation invitation, Member node)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(invitation, "Invitation is null");
        Reject.ifNull(node, "Node is null");

        controller.getTaskManager().scheduleTask(
            new SendMessageTask(invitation, node.getId()));

        if (!node.isCompletelyConnected()) {
            node.markForImmediateConnect();
            return false;
        }
        return true;
    }

    // Internal helper *********************************************************

    /** true if none acsii chars are found in string */
    private static final boolean containsNoneAscii(String str) {
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            if (c == 63 || c > 255) { // 63 = ?
                return true;
            }
        }
        return false;
    }
}
