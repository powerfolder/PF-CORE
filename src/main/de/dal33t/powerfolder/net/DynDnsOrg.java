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
package de.dal33t.powerfolder.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.StringUtils;

/*
 * The class DynDnsOrg is implemented to provide update service for those
 * with dynamic IP addresses in "http://www.dyndns.org".
 *
 * @author Albena Roshelova
 */

public class DynDnsOrg extends PFComponent implements DynDns {

    // error code
    private static final int GOOD = 0;
    private static final int NOCHG = 1;
    private static final int BADSYS = 2;
    private static final int BADAGENT = 3;
    private static final int BADAUTH = 4;
    private static final int NOTDONATOR = 5;
    private static final int NOTFQDN = 6;
    private static final int NOHOST = 7;
    private static final int NOTYOURS = 8;
    private static final int ABUSE = 9;
    private static final int NUMHOST = 10;
    private static final int DNSERR = 11;
    private static final int NINE11 = 12;

    // private PreferencesPanel panel;
    private ErrorManager errManager;
    // private Hashtable errors;
    private DynDnsManager manager;

    private String serverResp;
    private int result;

    public DynDnsOrg(Controller controller) {

        super(controller);

        errManager = new ErrorManager();
        // panel = new PreferencesPanel(controller);

        errManager.errors.put("good", new ErrorInfo("good", GOOD,
            ErrorManager.NO_ERROR,
            "The update was successful, and the hostname is now updated."));
        errManager.errors
            .put(
                "nochg",
                new ErrorInfo(
                    "nochg",
                    NOCHG,
                    ErrorManager.WARN,
                    "The update changed no settings, and is considered abusive. "
                        + "Additional updates will cause the hostname to become blocked."));

        errManager.errors.put("badsys", new ErrorInfo("badsys", BADSYS,
            ErrorManager.ERROR,
            "The system parameter given is not valid. Valid "
                + "system parameters are dyndns, statdns and custom"));

        errManager.errors
            .put(
                "badagent",
                new ErrorInfo(
                    "badagent",
                    BADAGENT,
                    ErrorManager.ERROR,
                    "The user agent that was sent has been blocked for "
                        + "not following these specifications or no user agent was specified"));

        errManager.errors.put("badauth", new ErrorInfo("badauth", BADAUTH,
            ErrorManager.ERROR,
            "The username or password specified are incorrect."));

        errManager.errors.put("!donator", new ErrorInfo("!donator", NOTDONATOR,
            ErrorManager.ERROR,
            "An option available only to credited users (such as offline URL)"
                + "was specified, but the user is not a credited user"));

        errManager.errors.put("notfqdn", new ErrorInfo("notfqdn", NOTFQDN,
            ErrorManager.ERROR,
            "The hostname specified is not a fully-qualified domain name "
                + "(not in the form hostname.dyndns.org or domain.com)"));

        errManager.errors
            .put(
                "nohost",
                new ErrorInfo(
                    "nohost",
                    NOHOST,
                    ErrorManager.ERROR,
                    "The hostname specified does not exist "
                        + "(or is not in the service specified in the system parameter)."));
        errManager.errors
            .put(
                "!yours",
                new ErrorInfo("!yours", NOTYOURS, ErrorManager.ERROR,
                    "The hostname specified exists, but not under the username specified."));

        errManager.errors.put("abuse", new ErrorInfo("abuse", ABUSE,
            ErrorManager.ERROR,
            "The hostname specified is blocked for update abuse."));

        errManager.errors.put("numhost", new ErrorInfo("numhost", NUMHOST,
            ErrorManager.ERROR, "Too many or too few hosts found."));

        errManager.errors.put("dnserr", new ErrorInfo("dnserr", DNSERR,
            ErrorManager.ERROR, "DNS error encountered."));

        errManager.errors.put("911", new ErrorInfo("911", NINE11,
            ErrorManager.ERROR,
            "There is a serious problem on our side, such as a database or "
                + "DNS server failure. The client should stop updating until "
                + "notified via the status page that the service is back up."));

    }

    /*
     * @see de.dal33t.powerfolder.net.DynDns#getDynDnsUpdateData()
     */
    public DynDnsUpdateData getDynDnsUpdateData() {

        DynDnsUpdateData data = new DynDnsUpdateData();
        manager.fillDefaultUpdateData(data);
        return data;
    }

    /*
     * @see de.dal33t.powerfolder.net.DynDns#setDynDnsManager(de.dal33t.powerfolder.net.DynDnsManager)
     */

    public void setDynDnsManager(DynDnsManager manager) {
        this.manager = manager;
    }

    /*
     * The method provides update service for those with DynDns IP adresses. It
     * updates the host IP to DynDns IP. @return an int of the
     * http://www.dyndns.org server response
     *
     * @see de.dal33t.powerfolder.net.DynDns#update(DynDnsUpdateData updateData)
     */
    public int update(DynDnsUpdateData updateData) {
        SocketChannel channel = null;
        Pattern p = null;
        Matcher m = null;
        String host2update = updateData.host;
        String host = "members.dyndns.org";
        String newIP = updateData.ipAddress; // getController().getDynDnsManager().getDyndnsViaHTTP();
        String accountPasswordStr = updateData.username + ":" + updateData.pass;
        byte[] accountPassword = accountPasswordStr.getBytes();

        // server responses
        String[] array = {"good", "nochg", "badsys", "badagent", "badauth",
            "notdonator", "notfqdn", "nohost", "notyours", "abuse", "numhost",
            "dnserr", "nine11"};

        String request = "GET /nic/update?" + "system=dyndns" + "&hostname="
            + host2update + "&myip=" + newIP + "&wildcard=ON" + "&offline=NO "
            + "HTTP/1.1\r\n" + "Host: members.dyndns.org\r\n"
            + "Authorization: Basic " + Base64.encodeBytes(accountPassword)
            + "\r\n" + "User-Agent: .net dyndns client\r\n\r\n";

        try {
            // Setup
            InetSocketAddress socketAddress = new InetSocketAddress(host, 80);
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();
            CharsetEncoder encoder = charset.newEncoder();

            // Allocate buffers
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            CharBuffer charBuffer = CharBuffer.allocate(1024);

            // Connect
            channel = SocketChannel.open();
            channel.connect(socketAddress);

            // Send request
            channel.write(encoder.encode(CharBuffer.wrap(request)));

            serverResp = "";
            // Read response

            while ((channel.read(buffer)) != -1) {
                buffer.flip();
                // Decode buffer
                decoder.decode(buffer, charBuffer, false);
                // Display
                charBuffer.flip();
                // System.out.println(charBuffer);
                buffer.clear();
                // charBuffer.clear();
            }

            logFiner("DynDns update result" + charBuffer);

            // get the response

            /*
             * String[] split = charBuffer.toString().split("\n"); String resp[] =
             * split[1].split("\\s"); serverResp = resp[0]; result =
             * errManager.getType(serverResp);
             */

            for (int i = 0; i < array.length; i++) {
                p = Pattern.compile(array[i]);
                m = p.matcher(charBuffer.toString());

                if (m.find()) {
                    serverResp = charBuffer.toString().substring(m.start(),
                        m.end());
                    result = errManager.getType(serverResp);

                    break;
                }
            }
        } catch (UnknownHostException e) {
            logWarning(" " + e.toString());
        } catch (IOException e) {
            logWarning(" " + e.toString());
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    /*
     * @return the dyndns server error message
     *
     * @see de.dal33t.powerfolder.net.DynDns#getErrorShortText()
     */

    public String getErrorShortText() {
        int err = !StringUtils.isBlank(serverResp) ? errManager
            .getCode(serverResp) : -1;

        switch (err) {
            case GOOD :
            case NOCHG :
            case BADAUTH :
            case NOHOST :
            case NOTDONATOR :
            case NOTFQDN :
            case BADAGENT :
            case BADSYS :
            case NUMHOST :
            case NOTYOURS :
            case ABUSE :
            case DNSERR :
            case NINE11 :

                return errManager.getShortText(serverResp).toUpperCase();

            default :
                return "Unknown result from dyndns service";
        }
    }

    /*
     * @return the error text
     *
     * @see de.dal33t.powerfolder.net.DynDns#getErrorText()
     */
    public String getErrorText() {

        int err = !StringUtils.isBlank(serverResp) ? errManager
            .getCode(serverResp) : -1;

        switch (err) {
            case GOOD :
            case NOCHG :
            case BADAUTH :
            case NOHOST :
            case NOTDONATOR :
            case NOTFQDN :
            case BADAGENT :
            case BADSYS :
            case NUMHOST :
            case NOTYOURS :
            case ABUSE :
            case DNSERR :
            case NINE11 :

                return errManager.getText(serverResp);

            default :
                return "Unknown result from dyndns service";
        }
    }
}
