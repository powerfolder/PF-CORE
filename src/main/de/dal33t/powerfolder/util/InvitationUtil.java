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

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.task.SendInvitationTask;
import de.dal33t.powerfolder.util.ui.DialogFactory;

/**
 * methods for loading and saving powerfolder invitations
 * 
 * @see Invitation
 */
public class InvitationUtil {
    // No instances
    private InvitationUtil() {

    }
    private static final Logger LOG = Logger.getLogger(InvitationUtil.class);

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
        LOG.verbose("Loading invitation " + file);
        try {
            FileInputStream fIn = new FileInputStream(file);
            return load(fIn);
        } catch (IOException e) {
            LOG.error("Unable to read invitation file stream", e);
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
        LOG.verbose("Loading invitation from " + in);
        try {
            ObjectInputStream oIn = new ObjectInputStream(in);
            Invitation invitation = (Invitation) oIn.readObject();

            if (invitation.invitor == null) {
                // Old file version, has another member info at end
                // New invitation files have memberinfo inclueded in invitation
                try {
                    MemberInfo from = (MemberInfo) oIn.readObject();
                    if (invitation.invitor == null) {
                        // Use invitation
                        invitation.invitor = from;
                    }
                } catch (IOException e) {
                    // Ingnore
                }
            }

            in.close();

            return invitation;
        } catch (ClassCastException e) {
            LOG.error("Unable to read invitation file stream", e);
        } catch (IOException e) {
            LOG.error("Unable to read invitation file stream", e);
        } catch (ClassNotFoundException e) {
            LOG.error("Unable to read invitation file stream", e);
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
            LOG.error("Unable to write invitation file stream", e);
            return false;
        }
    }

    private static boolean save(Invitation invitation, OutputStream out) {
        LOG.verbose("Saving invitation to " + out);
        ObjectOutputStream oOut;
        try {
            oOut = new ObjectOutputStream(out);
            oOut.writeObject(invitation);
            oOut.close();
            return true;
        } catch (IOException e) {
            LOG.error("Unable to save invitation file stream", e);
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
                    .getTranslation("invitationfiles.description");
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
     * @return true if the email was sent
     */
    public static boolean invitationToMail(Controller controller,
        Invitation invitation, String to)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(invitation, "Invitation is null");

        JFrame parent = controller.getUIController().getMainFrame()
            .getUIComponent();

        if (to == null) {
            to = (String) JOptionPane.showInputDialog(parent, Translation
                .getTranslation("sendinvitation.ask_emailaddres.message"),
                Translation
                    .getTranslation("sendinvitation.ask_emailaddres.title"),
                JOptionPane.QUESTION_MESSAGE, null, null, Translation
                    .getTranslation("sendinvitation.example_emailaddress"));
        }

        // null if canceled
        if (to == null) {
            return false;
        }

        String filename = invitation.folder.name;
        // SendTo app needs simple chars as filename
        if (containsNoneAscii(filename)) {
            filename = "powerfolder";
        }
        String tmpDir = System.getProperty("java.io.tmpdir");
        File file;
        if (tmpDir != null && tmpDir.length() > 0) {
            // create in tmp dir if available
            file = new File(tmpDir, filename + ".invitation");
        } else {
            // else create in working directory
            file = new File(filename + ".invitation");
        }
        if (!save(invitation, file)) {
            LOG.error("sendmail failed");
            return false;
        }
        file.deleteOnExit();

        String invitationName = invitation.folder.name;
        String subject = Translation.getTranslation("sendinvitation.subject",
            invitationName);
        String body = Translation.getTranslation("sendinvitation.body", to,
            controller.getMySelf().getNick(), invitationName);
        if (!MailUtil.sendMail(to, subject, body, file)) {
            LOG.error("sendmail failed");
            file.delete();
            return false;
        }

        file.delete();
        return true;
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
    public static boolean invitationToDisk(Controller controller,
        Invitation invitation, File file)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(invitation, "Invitation is null");

        // Select file
        if (file == null) {
            JFileChooser fc = DialogFactory.createFileChooser();
            fc.setDialogTitle(Translation
                .getTranslation("sendinvitation.placetostore"));
            // Recommended file
            fc
                .setSelectedFile(new File(invitation.folder.name
                    + ".invitation"));
            fc.setFileFilter(InvitationUtil.createInvitationsFilefilter());
            int result = fc.showSaveDialog(controller.getUIController()
                .getMainFrame().getUIComponent());
            if (result != JFileChooser.APPROVE_OPTION) {
                return false;
            }

            // Store invitation to disk
            file = fc.getSelectedFile();
            if (file == null) {
                return false;
            }
            if (file.exists()) {
                // TODO: Add confirm dialog
            }
        }

        LOG.info("Writing invitation to " + file);
        if (!save(invitation, file)) {
            controller.getUIController().showErrorMessage(
                "Unable to write invitation",
                "Error while writing invitation, please try again.", null);
            return false;
        }

        return true;
    }

    /**
     * Sends an invitation to a connected node.
     * 
     * @param controller
     * @param invitation
     * @param node
     * @return true if invitation could be sent.
     */
    public static boolean invitationToNode(Controller controller,
        Invitation invitation, Member node)
    {
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(invitation, "Invitation is null");
        Reject.ifNull(node, "Node is null");

        controller.getTaskManager().scheduleTask(
        		new SendInvitationTask(invitation, node.getInfo()));
        
        if (!node.isCompleteyConnected()) {
            return false;
        }
//        node.sendMessageAsynchron(invitation, null);
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
