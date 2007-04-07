package de.dal33t.powerfolder.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
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
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }
        // sendto.exe usage :
        // sendto.exe -files <file1> <file2> ... -body <content> -to <email
        // address> -subject <content>
        // example : sendto.exe -files "c:\my files\file1.ppt" c:\document.doc

        // prepare params to give to SendTo.exe program
        String params = "";
        if (!StringUtils.isBlank(to)) {
            params += " -to " + to;
        }

        if (!StringUtils.isBlank(subject)) {
            params += " -subject \"" + subject + "\"";
        }

        if (!StringUtils.isBlank(body)) {
            params += " -body \"" + body + "\"";
        }

        if (attachment != null) {
            if (!attachment.exists()) {
                throw new IllegalArgumentException("sendmail file attachment ("
                    + attachment.getAbsolutePath() + ")does not exists");
            }
            if (!attachment.canRead()) {
                throw new IllegalArgumentException(
                    "sendmail file attachment not ("
                        + attachment.getAbsolutePath() + ") readable");
            }
            params += " -files \"" + attachment.getAbsolutePath() + "\"";
        }
        // extract exe file from jar

        try {
            File sendto = Util.copyResourceTo("SendTo.exe",
                "SendToApp/Release", Controller.getTempFilesLocation(), true);

            Process sendMail = Runtime.getRuntime().exec(
                sendto.getAbsolutePath() + " " + params + "");
            int result = sendMail.waitFor();
            LOG.debug("Sendto returned with: " + result);
            LOG.debug("Mail send");
            return true;
        } catch (IOException e) {
            LOG.warn("Unable to send mail " + e.getMessage());
            LOG.verbose(e);
            return false;
        } catch (InterruptedException e) {
            LOG.warn("Unable to send mail " + e.getMessage());
            LOG.verbose(e);
            return false;
        }
    }
}
