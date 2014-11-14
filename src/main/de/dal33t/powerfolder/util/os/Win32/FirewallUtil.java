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
package de.dal33t.powerfolder.util.os.Win32;

import java.io.IOException;
import java.io.PrintWriter;

import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * This class gives access to some functions of the windows firewall. Note that
 * some methods may take some seconds to complete.
 *
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 */
public class FirewallUtil {

    public static boolean isFirewallAccessible() {
        return OSUtil.isWindowsSystem() && !OSUtil.isWindowsMEorOlder();
    }

    /**
     * Tries to open a port on the windows firewall. In case of an error it
     * either doesn't return or it throws an IOException.
     *
     * @param port
     *            the port to open
     * @throws IOException
     *             in case of a problem
     */
    public static void openport(int port) throws IOException {
        openport(port, "TCP");
    }

    /**
     * Tries to open a port on the windows firewall. In case of an error it
     * either doesn't return or it throws an IOException.
     *
     * @param port
     *            the port to open
     * @param protocol
     *            the protocol. "TCP" or "UDP".
     * @throws IOException
     *             in case of a problem
     */
    public static void openport(int port, String protocol) throws IOException {
        Process netsh;
        PrintWriter nout = null;

        netsh = Runtime.getRuntime().exec("netsh");
        try {
            String appname = Translation
                .getTranslation("general.application.name");
            nout = new PrintWriter(netsh.getOutputStream(), true);

            // Source: http://support.microsoft.com/kb/947709/en-us
            String preWin7cl = "firewall add portopening protocol="
                + protocol.toUpperCase() + " port=" + port + " name=\""
                + appname + "\"";
            String win7cl = "advfirewall firewall add rule name=\"" + appname
                + " (" + port + "/" + protocol.toUpperCase()
                + ")\" dir=in action=allow protocol=" + protocol.toUpperCase()
                + " localport=" + port;

            // Requires elevated rights: nout.println(win7cl);
            nout.println(preWin7cl);
            nout.println("exit");

            byte[] bOut = StreamUtils.readIntoByteArray(netsh.getInputStream());
            String reply = new String(bOut, Convert.UTF8);
            if (reply == null || !reply.toUpperCase().contains("OK")) {
                throw new IOException(reply);
            }
            try {
                int res = netsh.waitFor();
                if (res != 0)
                    throw new IOException("netsh returned " + res);
            } catch (InterruptedException e) {
                throw (IOException) new IOException(e.toString()).initCause(e);
            }
        } finally {
            if (nout != null) {
                nout.close();
            }
        }
    }

    /**
     * Tries to close a port on the windows firewall. In case of an error it
     * either doesn't return or it throws an IOException.
     *
     * @param port
     *            the port to close
     * @throws IOException
     *             in case of a problem
     */
    public static void closeport(int port) throws IOException {
        closeport(port, "TCP");
    }

    /**
     * Tries to close a port on the windows firewall. In case of an error it
     * either doesn't return or it throws an IOException.
     *
     * @param port
     *            the port to close
     * @throws IOException
     *             in case of a problem
     */
    public static void closeport(int port, String protocol) throws IOException {
        Process netsh;
        PrintWriter nout = null;

        netsh = Runtime.getRuntime().exec("netsh");
        try {
            nout = new PrintWriter(netsh.getOutputStream(), true);
            nout.println("firewall delete portopening protocol=" + protocol
                + " port=" + port);
            nout.println("bye");

            byte[] bOut = StreamUtils.readIntoByteArray(netsh.getInputStream());
            String reply = new String(bOut, Convert.UTF8);

            if (reply == null || !reply.toUpperCase().contains("OK")) {
                throw new IOException(reply);
            }
            try {
                int res = netsh.waitFor();
                if (res != 0)
                    throw new IOException("netsh returned " + res);
            } catch (InterruptedException e) {
                throw (IOException) new IOException(e.toString()).initCause(e);
            }
        } finally {
            if (nout != null) {
                nout.close();
            }
        }
    }

    /**
     * Tries to close one port an open another one on the windows firewall. In
     * case of an error it either doesn't return or it throws an IOException.
     *
     * @param from
     * @param to
     * @throws IOException
     *             in case of a problem
     */
    public static void changeport(int from, int to) throws IOException {
        closeport(from);
        openport(to);
    }
}
