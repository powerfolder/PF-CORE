package de.dal33t.powerfolder.util;

import java.io.*;

import javax.swing.filechooser.FileFilter;

import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;

/** methods for loading and saving powerfolder invitations
 * @see Invitation 
 **/
public class InvitationUtil {

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
    private static Invitation load(InputStream in) {
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
     */
    public static void save(Invitation invitation, File file) {
        try {
            save(invitation, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            LOG.error("Unable to write invitation file stream", e);
        }
    }

    private static void save(Invitation invitation, OutputStream out) {
        LOG.verbose("Saving invitation to " + out);
        ObjectOutputStream oOut;
        try {
            oOut = new ObjectOutputStream(out);
            oOut.writeObject(invitation);
            oOut.close();
        } catch (IOException e) {
            LOG.error("Unable to save invitation file stream", e);
        }
    }

    /**
     * Creates a file filter for powerfolder invitations
     * 
     * @return
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

}
