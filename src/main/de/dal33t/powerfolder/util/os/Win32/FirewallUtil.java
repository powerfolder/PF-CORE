package de.dal33t.powerfolder.util.os.Win32;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import de.dal33t.powerfolder.util.OSUtil;

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
        Process netsh;
        BufferedReader nin;
        PrintWriter nout;

        netsh = Runtime.getRuntime().exec("netsh");
        nin = new BufferedReader(new InputStreamReader(netsh.getInputStream()));
        nout = new PrintWriter(netsh.getOutputStream(), true);
        nout.println("firewall add portopening protocol=TCP port=" + port
            + " name=\"PowerFolder\"");
        String reply = nin.readLine();
        if (!reply.equalsIgnoreCase("netsh>Ok.")) {
            throw new IOException(reply);
        }
        nout.println("bye");
        try {
            int res = netsh.waitFor();
            if (res != 0)
                throw new IOException("netsh returned " + res);
        } catch (InterruptedException e) {
            throw (IOException) new IOException(e.toString()).initCause(e);
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
        Process netsh;
        BufferedReader nin;
        PrintWriter nout;

        netsh = Runtime.getRuntime().exec("netsh");
        nin = new BufferedReader(new InputStreamReader(netsh.getInputStream()));
        nout = new PrintWriter(netsh.getOutputStream(), true);
        nout.println("firewall delete portopening protocol=TCP port=" + port);
        String reply = nin.readLine();
        if (!reply.equalsIgnoreCase("netsh>Ok.")) {
            throw new IOException(reply);
        }
        nout.println("bye");
        try {
            int res = netsh.waitFor();
            if (res != 0)
                throw new IOException("netsh returned " + res);
        } catch (InterruptedException e) {
            throw (IOException) new IOException(e.toString()).initCause(e);
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
