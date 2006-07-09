/* $Id: DynDnsManager.java,v 1.6 2006/04/30 19:32:06 schaatser Exp $
 */
package de.dal33t.powerfolder.net;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.dialog.ErrorDialog;
import de.dal33t.powerfolder.ui.preferences.DynDnsSettingsTab;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * The DynDnsManager class is responsible for: - to provide services to the
 * DynDns updates and, - DynDns validation as well as some UI utility methods.
 * 
 * @author Albena Roshelova
 */

public class DynDnsManager extends PFComponent {

    private Hashtable dynDnsTable;
    public DynDns activeDynDns;
    public String externalIP;
    private ErrorDialog errorDialog;

    private JDialog uiComponent;

    public DynDnsManager(Controller controller) {
        super(controller);

        RegisterDynDns("DynDnsOrg", new DynDnsOrg(controller));
        errorDialog = new ErrorDialog(controller, true);
    }

    /*
     * RegisterDynDns methods register the dyndns source
     */
    public void RegisterDynDns(String dynDnsId, DynDns dynDns) {
        dynDnsTable = new Hashtable();
        dynDns.setDynDnsManager(this);
        dynDnsTable.put(dynDnsId, dynDns);
    }

    public String getUsername() {
        if (DynDnsSettingsTab.username == null) {
            return ConfigurationEntry.DYNDNS_USERNAME.getValue(getController());
        }
        return DynDnsSettingsTab.username;
    }

    public String getUserPassword() {
        if (DynDnsSettingsTab.password == null) {
            return ConfigurationEntry.DYNDNS_PASSWORD.getValue(getController());
        }
        return DynDnsSettingsTab.password;
    }

    public String getHost2Update() {
        if (DynDnsSettingsTab.newDyndns == null) {
            return ConfigurationEntry.DYNDNS_HOSTNAME.getValue(getController());
        }
        return DynDnsSettingsTab.newDyndns;
    }

    public void fillDefaultUpdateData(DynDnsUpdateData updateData) {
        updateData.username = getUsername();
        updateData.pass = getUserPassword();
        updateData.host = getHost2Update();
    }

    /**
     * Validates given dynDns for compatibility with the current host
     * 
     * @param dynDns
     *            to validate
     * @return true if validation succeeded, false otherwise
     */
    public boolean validateDynDns(String dynDns) {

        // validates the dynamic dns entry if there is one entered
        if (!StringUtils.isBlank(dynDns)) {
            if (getController().getConnectionListener() != null) {

                // sets the new dyndns with validation enabled
                int res = getController().getConnectionListener().setMyDynDns(
                    dynDns, true);

                // check the result from validation
                switch (res) {
                    case ConnectionListener.VALIDATION_FAILED :

                        // validation failed ask the user if he/she
                        // wants to continue with these settings
                        String message = Translation
                            .getTranslation("preferences.dialog.dyndnsmanager.nomatch.text");
                        String title = Translation
                            .getTranslation("preferences.dialog.dyndnsmanager.nomatch.title");

                        int result = JOptionPane.showConfirmDialog(
                            getController().getUIController().getMainFrame()
                                .getUIComponent(), message, title,
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                        if (result == JOptionPane.YES_OPTION) {
                            // the user is happy with his/her settings, then
                            // set the new dyndns without further validation
                            getController().getConnectionListener()
                                .setMyDynDns(dynDns, false);
                        } else {
                            // the user wants to change the dyndns settings
                            getController().getConnectionListener()
                                .setMyDynDns(null, false);
                            return false;
                        }
                        break;
                    case ConnectionListener.CANNOT_RESOLVE :
                        // the new dyndns could not be resolved
                        // force the user to enter a new one
                        getController().getConnectionListener().setMyDynDns(
                            null, false);
                        return false;

                    case ConnectionListener.OK :
                        log().info(
                            "Successfully validated dyndns '" + dynDns + "'");
                // getController().getUIController()
                // .showMessage(null,
                // "Success",
                // Translation.getTranslation("preferences.dialog.statusDynDnsSuccess",
                // dynDns));
                }
            }

        } else {
            // just resets the dyndns entry
            if (getController().getConnectionListener() != null) {
                getController().getConnectionListener()
                    .setMyDynDns(null, false);
            }
        }
        // all validations have passed
        return true;
    }

    /**
     * Shows warning message to the user in case the validation goes wrong
     * 
     * @param type
     *            of validation failure
     * @param arg
     *            additional argument (dyndns)
     */
    public void showWarningMsg(int type, String arg) {
        switch (type) {
            case ConnectionListener.VALIDATION_FAILED :
                getController().getUIController().showWarningMessage(
                    Translation
                        .getTranslation("preferences.dialog.warnningMessage"),
                    Translation.getTranslation(
                        "preferences.dialog.statusValidFailed", arg));
                break;

            case ConnectionListener.CANNOT_RESOLVE :
                getController().getUIController().showWarningMessage(
                    Translation
                        .getTranslation("preferences.dialog.warnningMessage"),
                    Translation.getTranslation(
                        "preferences.dialog.statusValidFailed", arg));
        }
    }

    public void showPanelErrorMessage() {
        String err = "";
        if (getHost2Update().equals(""))
            err = "hostname";
        else if (getUsername().equals(""))
            err = "username";
        else if (getUserPassword().equals(""))
            err = "password";

        getController().getUIController().showErrorMessage(
            Translation.getTranslation("preferences.dialog.dyndnsUpdateTitle"),
            "The field " + err + " can not be empty!", null);
    }

    /**
     *
     */
    public void showDynDnsUpdaterMsg(int type) {

        switch (type) {
            case ErrorManager.NO_ERROR :
                getController()
                    .getUIController()
                    .showMessage(
                        null,
                        Translation
                            .getTranslation("preferences.dialog.dyndnsUpdateTitle"),
                        activeDynDns.getErrorText());
                break;

            case ErrorManager.WARN :
            case ErrorManager.ERROR :
                errorDialog.open(activeDynDns.getErrorText(), type);
                break;

            case ErrorManager.UNKNOWN :
                getController()
                    .getUIController()
                    .showErrorMessage(
                        Translation
                            .getTranslation("preferences.dialog.dyndnsUpdateTitle"),
                        Translation
                            .getTranslation("preferences.dialog.dyndnsUpdateUnknowError"),
                        null);
                break;

        }
    }

    /**
     * close the wait message box
     */
    public final void close() {
        log().verbose("Close called: " + this);
        if (uiComponent != null) {
            uiComponent.dispose();
            uiComponent = null;
        }
    }

    /**
     * Shows (and builds) the wait message box
     */
    public final void show(String dyndns) {
        log().verbose("Open called: " + this);
        getUIComponent(dyndns).setVisible(true);
    }

    /**
     * retrieves the title of the message box
     * 
     * @return
     */
    private String getTitle() {
        return Translation.getTranslation("preferences.dialog.titleProcessing");
    }

    /**
     * saves updated ip to the config file
     */
    private void saveUpdatedIP() {
        ConfigurationEntry.DYNDNS_LAST_UPDATED_UP.setValue(getController(),
            getDyndnsViaHTTP());
        // save
        getController().saveConfig();
    }

    /**
     * Setups the UI for the wait message box
     * 
     * @param dyndns
     * @return
     */
    protected final JDialog getUIComponent(String dyndns) {
        if (uiComponent == null) {
            log().verbose("Building ui component for " + this);
            uiComponent = new JDialog(getController().getUIController()
                .getMainFrame().getUIComponent(), getTitle());
            uiComponent.setResizable(false);

            uiComponent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            FormLayout layout = new FormLayout("pref, 14dlu, pref:grow",
                "pref, pref:grow, 7dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU14_BORDER);

            CellConstraints cc = new CellConstraints();

            // Build
            int xpos = 1, ypos = 1, wpos = 1, hpos = 1;
            builder.add(new JLabel(Translation.getTranslation(
                "preferences.dialog.statusWaitDynDns", dyndns)), cc.xywh(xpos,
                ypos, wpos, hpos));

            // Add panel to component
            uiComponent.getContentPane().add(builder.getPanel());

            uiComponent.pack();
            Component parent = uiComponent.getOwner();
            int x = parent.getX()
                + (parent.getWidth() - uiComponent.getWidth()) / 2;
            int y = parent.getY()
                + (parent.getHeight() - uiComponent.getHeight()) / 2;
            uiComponent.setLocation(x, y);
        }
        return uiComponent;
    }

    public boolean ipCheck() {
        String currentDyndnsIP = getHostIP(ConfigurationEntry.DYNDNS_HOSTNAME
            .getValue(getController()));
        String myHostIP = getDyndnsViaHTTP();

        if (currentDyndnsIP.equals("") && myHostIP.equals(""))
            return false;

        if (!currentDyndnsIP.equals("") && !myHostIP.equals("")) {
            if (!myHostIP.equals(currentDyndnsIP)) {
                return false;
            }
        }
        return true;
    }

    public void onStartUpdate() {
        if (!ipCheck()) {
            activeDynDns = (DynDns) dynDnsTable.get("DynDnsOrg");
            DynDnsUpdateData updateData = activeDynDns.getDynDnsUpdateData();
            int res = activeDynDns.update(updateData);

            if (res == ErrorManager.ERROR || res == ErrorManager.UNKNOWN) {
                showDynDnsUpdaterMsg(res);
            }
            if (res == ErrorManager.NO_ERROR) {
                saveUpdatedIP();
            }
        }
    }

    public void forceUpdate() {
        log().verbose("start dyndns updater");

        activeDynDns = (DynDns) dynDnsTable.get("DynDnsOrg");
        DynDnsUpdateData updateData = activeDynDns.getDynDnsUpdateData();
        int res = activeDynDns.update(updateData);

        if (res == ErrorManager.NO_ERROR) {
            saveUpdatedIP();
        }

        showDynDnsUpdaterMsg(res);

        log().verbose("the updated dyndns > " + externalIP);
    }

    /**
     * retruns dyndns IP address
     * 
     * @param newDns
     */

    public String getHostIP(String host) {

        String strDyndnsIP = "";

        if (host == null)
            return "";

        try {
            InetSocketAddress myDyndns = new InetSocketAddress(host, 0); // port
            InetAddress myDyndnsIP = myDyndns.getAddress();
            if (myDyndnsIP != null) {
                strDyndnsIP = myDyndnsIP.getHostAddress();
            }
        } catch (IllegalArgumentException ex) {
            log().error("Can't get the host ip address" + ex.toString());
        }
        return strDyndnsIP;
    }

    /**
     * Returns the internet address of this machine.
     * 
     * @return the ip address or empty string if none is found
     */

    public String getDyndnsViaHTTP() {

        String ipAddr = "";

        try {
            URL dyndns = new URL("http://checkip.dyndns.org/");
            URLConnection urlConn = dyndns.openConnection();
            int length = urlConn.getContentLength();
            ByteArrayOutputStream tempBuffer;

            if (length < 0) {
                tempBuffer = new ByteArrayOutputStream();
            } else {
                tempBuffer = new ByteArrayOutputStream(length);
            }

            InputStream inStream = urlConn.getInputStream();

            int ch;
            while ((ch = inStream.read()) >= 0) {
                tempBuffer.write(ch);
            }

            String ipText = tempBuffer.toString();
            Logger.getLogger(ConnectionListener.class).warn(
                "Received '" + ipText + "' from " + dyndns);
            ipAddr = filterIPs(ipText);

        } catch (IOException e) {
        }

        // return the ip address or empty string if none is found
        return ipAddr;
    }

    /**
     * Parse the HTML string and filter everything out but the ip address
     * 
     * @param str
     * @return
     */
    private String filterIPs(String txt) {
        String ip = null;
        Pattern p = Pattern
            .compile("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}");
        Matcher m = p.matcher(txt);

        if (m.find()) {
            // ip match is found
            ip = txt.substring(m.start(), m.end());
        }
        return ip;
    }

}