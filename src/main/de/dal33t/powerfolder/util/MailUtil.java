package de.dal33t.powerfolder.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.jdic.desktop.DesktopException;
import org.jdesktop.jdic.desktop.Message;

import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Utility class for sending emails.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class MailUtil {

    private static final Logger LOG = Logger.getLogger(MailUtil.class);

    // No instance possible
    private MailUtil() {
    }

    /**
     * @see #sendMail(String, String, String, File)
     * @return true when sending mails by the default email program is
     *         available.
     */
    public static boolean isSendEmailAvailable() {
        return OSUtil.isWindowsSystem();
    }

    /**
     * Starts default mail program on <B>Windows</B> with prepared message.
     * Note that the body cannot be long. Parameters can be ommitted (give null
     * as parameter). {@link #isSendEmailAvailable()} must return true for this
     * to work. This is tested on win2000, winXp and Vista. If no <B>to</B>
     * parameter is set the SendTo.exe program wil set a default (something like
     * mail@domain.com).
     * 
     * @param to
     *            The optional recipent (email address) to send the mail to. If
     *            no <B>to</B> parameter is set the SendTo.exe program wil set
     *            a default (something like mail@domain.com).
     * @param subject
     *            The optional subject of the mail
     * @param body
     *            The optional body of the mail, cannot be very long please test
     *            before use!
     * @param attachment
     *            The optional file to attatch
     * @see Util#isWindowsSystem()
     */
    public static boolean sendMail(String to, String subject, String body,
        File attachment)
    {
        Message msg = new Message();
        if (!StringUtils.isBlank(to)) {
            msg.setToAddrs(Arrays.asList(new String[] { to }));
        }
        
        if (!StringUtils.isBlank(subject)) {
            msg.setSubject(subject);
        }
        
        if (!StringUtils.isBlank(body)) {
            msg.setBody(body);
        }
        
        if (attachment != null) {
            try {
                msg.setAttachments(Arrays.asList(new String[] { attachment.getAbsolutePath() }));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                    "sendmail file attachment ("
                        + attachment.getAbsolutePath() + ") not readable or not existant.");
            }
        }
        try {
            org.jdesktop.jdic.desktop.Desktop.mail(msg);
        } catch (DesktopException e) {
            LOG.error(e);
            return false;
        }
        return true;
    }
}
