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
 * $Id: InvitationUtil.java 20526 2012-12-14 05:24:46Z glasgow $
 */
package de.dal33t.powerfolder.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.filechooser.FileFilter;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.SendInvitationEmail;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.task.SendMessageTask;

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

        controller.getOSClient().getFolderService(invitation.folder).sendInvitationEmail(
                new SendInvitationEmail(invitation, to, ccMe));
    }

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
}
