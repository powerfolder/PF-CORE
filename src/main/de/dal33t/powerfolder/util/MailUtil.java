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

import de.dal33t.powerfolder.util.os.OSUtil;
import org.apache.commons.lang.StringUtils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for sending emails.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class MailUtil {

    private static final Logger log = Logger.getLogger(MailUtil.class.getName());

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
        try {
            if (!Desktop.isDesktopSupported()) {
                return false;
            }
        } catch (LinkageError err) {
            log.log(Level.FINER, "LinkageError", err);
            return false;
        }
        
        StringBuilder headers = new StringBuilder();

        try {
            char separator = '?';
            if (!StringUtils.isBlank(to)) {
                headers.append(separator).append("to=").append(Util.encodeURI(to));
                separator = '&';
            }
            
            if (!StringUtils.isBlank(subject)) {
                headers.append(separator).append("subject=").append(Util.encodeURI(subject));
                separator = '&';
            }
    
            if (!StringUtils.isBlank(body)) {
                headers.append(separator).append("body=").append(Util.encodeURI(body));
                separator = '&';
            }
            
            if (attachment != null) {
                headers.append(separator).append("attachments=").append(Util.encodeURI(attachment.getAbsolutePath()));
                separator = '&';
            }

            log.finer("mailto:" + headers);
            Desktop.getDesktop().mail(new URI("mailto:" + headers));
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "UnsupportedEncodingException", e);
            return false;
        } catch (IOException e) {
            log.log(Level.SEVERE, "IOException", e);
            return false;
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }
}
