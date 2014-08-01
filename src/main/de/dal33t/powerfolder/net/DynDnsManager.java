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

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.dialog.ErrorDialog;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.preferences.DynDnsSettingsTab;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * The DynDnsManager class is responsible for: - to provide services to the
 * DynDns updates and, - DynDns validation as well as some UI utility methods.
 *
 * @author Albena Roshelova
 */

public class DynDnsManager extends PFComponent {

    private static final long DYNDNS_TIMER_INTERVAL = 1000 * 60 * 5;
    private TimerTask updateTask;
    private Thread updateThread;

    private Hashtable<String, DynDns> dynDnsTable;
    public DynDns activeDynDns;
    public String externalIP;
    private ErrorDialog errorDialog;

    private JDialog uiComponent;

    public DynDnsManager(Controller controller) {
        super(controller);

        registerDynDns("DynDnsOrg", new DynDnsOrg(controller));
        errorDialog = new ErrorDialog(controller, true);
    }

    /*
     * RegisterDynDns methods register the dyndns source
     */
    public void registerDynDns(String dynDnsId, DynDns dynDns) {
        dynDnsTable = new Hashtable<String, DynDns>();
        dynDns.setDynDnsManager(this);
        dynDnsTable.put(dynDnsId, dynDns);
    }

    public String getUsername() {
        if (DynDnsSettingsTab.getUsername() == null) {
            return ConfigurationEntry.DYNDNS_USERNAME.getValue(getController());
        }
        return DynDnsSettingsTab.getUsername();
    }

    public String getUserPassword() {
        if (DynDnsSettingsTab.getPassword() == null) {
            return ConfigurationEntry.DYNDNS_PASSWORD.getValue(getController());
        }
        return DynDnsSettingsTab.getPassword();
    }

    public String getHost2Update() {
        if (DynDnsSettingsTab.getNewDyndns() == null) {
            return ConfigurationEntry.HOSTNAME.getValue(getController());
        }
        return DynDnsSettingsTab.getNewDyndns();
    }

    public boolean isDynDnsSet() {
        return !StringUtils.isBlank(getHost2Update());
    }

    public void fillDefaultUpdateData(DynDnsUpdateData updateData) {
        updateData.username = getUsername();
        updateData.pass = getUserPassword();
        updateData.host = getHost2Update();
        updateData.ipAddress = getIPviaHTTPCheckIP();
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
        if (StringUtils.isBlank(dynDns)) {
            // just resets the dyndns entry
            if (getController().getConnectionListener() != null) {
                getController().getConnectionListener()
                    .setMyDynDns(null, false);
            }
        } else {
            if (getController().hasConnectionListener()) {

                // sets the new dyndns with validation enabled
                int res = getController().getConnectionListener().setMyDynDns(
                    dynDns, true);

                // check the result from validation
                switch (res) {
                    case ConnectionListener.VALIDATION_FAILED :

                        // validation failed ask the user if he/she
                        // wants to continue with these settings
                        String message = Translation
                            .getTranslation("exp.preferences.dyn_dns.manager_no_match_text");
                        String title = Translation
                            .getTranslation("exp.preferences.dyn_dns.manager_no_match_title");

                        int result = DialogFactory.genericDialog(
                            getController(), title, message, new String[]{
                                Translation.getTranslation("general.continue"),
                                Translation.getTranslation("general.cancel")},
                            0, GenericDialogType.WARN); // Default is
                        // continue

                        if (result == 0) { // Continue
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
                        logInfo("Successfully validated dyndns '" + dynDns
                            + '\'');
                        // getController().getUIController()
                        // .showMessage(null,
                        // "Success",
                        // Translation.getTranslation("preferences.dialog.statusDynDnsSuccess",
                        // dynDns));
                }
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

                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("exp.preferences.dyn_dns.warning_message"),
                    Translation.getTranslation(
                            "exp.preferences.dyn_dns.status_valid_failed", arg),
                    GenericDialogType.WARN);

                break;

            case ConnectionListener.CANNOT_RESOLVE :
                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("exp.preferences.dyn_dns.warning_message"),
                    Translation.getTranslation(
                            "exp.preferences.dyn_dns.status_valid_failed", arg),
                    GenericDialogType.WARN);

        }
    }

    public void showPanelErrorMessage() {
        String err = "";
        if (getHost2Update().length() == 0) {
            err = "hostname";
        } else if (getUsername().length() == 0) {
            err = "username";
        } else if (getUserPassword().length() == 0) {
            err = "password";
        }

        DialogFactory.genericDialog(getController(), Translation
            .getTranslation("exp.preferences.dyn_dns.update_title"),
            Translation.getTranslation("exp.preferences.dyn_dns.update_text",
                err), GenericDialogType.ERROR);
    }

    /**
     *
     */
    public void showDynDnsUpdaterMsg(int type) {

        switch (type) {
            case ErrorManager.NO_ERROR :

                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("exp.preferences.dyn_dns.update_title"),

                activeDynDns.getErrorText(), GenericDialogType.INFO);
                break;

            case ErrorManager.WARN :
            case ErrorManager.ERROR :
                errorDialog.open(activeDynDns.getErrorText(), type);
                break;

            case ErrorManager.UNKNOWN :
                DialogFactory
                    .genericDialog(
                        getController(),
                        Translation
                            .getTranslation("exp.preferences.dyn_dns.update_title"),
                        Translation
                            .getTranslation("exp.preferences.dyn_dns.update_unknown_error"),
                        GenericDialogType.ERROR);
                break;

        }
    }

    /**
     * close the wait message box
     */
    public final void close() {
        logFiner("Close called: " + this);
        if (uiComponent != null) {
            uiComponent.dispose();
            uiComponent = null;
        }
    }

    /**
     * Shows (and builds) the wait message box
     */
    public final void show(String dyndns) {
        logFiner("Open called: " + this);
        getUIComponent(dyndns).setVisible(true);
    }

    /**
     * retrieves the title of the message box
     *
     * @return
     */
    private String getTitle() {
        return Translation.getTranslation("preferences.dialog.title_processing");
    }

    /**
     * saves updated ip to the config file
     *
     * @param updateData
     */
    private void saveUpdatedIP(DynDnsUpdateData updateData) {
        ConfigurationEntry.DYNDNS_LAST_UPDATED_IP.setValue(getController(),
            updateData.ipAddress);
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
            logFiner("Building ui component for " + this);
            uiComponent = new JDialog(getController().getUIController()
                .getMainFrame().getUIComponent(), getTitle());
            uiComponent.setResizable(false);

            uiComponent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            FormLayout layout = new FormLayout("pref, 14dlu, pref:grow",
                "pref, pref:grow, 6dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU14_BORDER);

            CellConstraints cc = new CellConstraints();

            // Build
            int xpos = 1;
            int ypos = 1;
            int wpos = 1;
            int hpos = 1;
            builder.add(new JLabel(Translation.getTranslation(
                    "exp.preferences.dyn_dns.status_wait", dyndns)), cc.xywh(xpos,
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

    /**
     * Checks if the current dyndns host is still valid (=matches the real ip).
     *
     * @return false, if the dyndns service should be updated.
     */
    private boolean dyndnsValid() {
        String dyndnsIP = getHostIP(ConfigurationEntry.HOSTNAME
            .getValue(getController()));
        if (StringUtils.isEmpty(dyndnsIP)) {
            return true;
        }

        String myHostIP = getIPviaHTTPCheckIP();
        String lastUpdatedIP = ConfigurationEntry.DYNDNS_LAST_UPDATED_IP
            .getValue(getController());
        logFine("Dyndns hostname IP: " + dyndnsIP + ". Real IP: " + myHostIP
            + ". Last update IP: " + lastUpdatedIP);
        if (dyndnsIP.equals(myHostIP)) {
            return true;
        }
        // If host did non change...
        return myHostIP.equals(lastUpdatedIP);

    }

    /**
     * Forces an update of the DynDNS service.
     *
     * @return The update result
     */
    private int updateDynDNS() {
        activeDynDns = dynDnsTable.get("DynDnsOrg");
        DynDnsUpdateData updateData = activeDynDns.getDynDnsUpdateData();
        int res = activeDynDns.update(updateData);

        logInfo("Updated dyndns. Result: " + res);
        if (res == ErrorManager.NO_ERROR) {
            saveUpdatedIP(updateData);
        }

        logFiner("the updated dyndns > " + externalIP);
        return res;
    }

    public void forceUpdate() {
        showDynDnsUpdaterMsg(updateDynDNS());
    }

    /**
     * Updates DYNDNS if necessary.
     */
    public synchronized void updateIfNessesary() {
        if (!ConfigurationEntry.DYNDNS_AUTO_UPDATE
            .getValueBoolean(getController()))
        {
            return;
        }
        if (updateTask == null) {
            setupUpdateTask();
            logFiner("DNS Autoupdate requested. Starting updater.");
        }

        if (updateThread != null) {
            logFine("No dyndns update performed. Already running");
            return;
        }

        // Perform this by a seperate Thread
        updateThread = new Thread("DynDns Updater") {
            @Override
            public void run() {
                boolean dyndnsIsValid = dyndnsValid();
                logFine("Dyndns updater start. Update required? "
                    + !dyndnsIsValid);
                if (dyndnsIsValid) {
                    logFiner("No dyndns update performed: IP still valid");
                } else {
                    updateDynDNS();
                }
                logFiner("Dyndns updater finished");
                updateThread = null;
            }
        };
        updateThread.start();
    }

    /**
     * Start updating Timer.
     */
    private void setupUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = new TimerTask() {
            @Override
            public void run() {
                updateIfNessesary();
            }
        };
        getController().scheduleAndRepeat(updateTask, 0, DYNDNS_TIMER_INTERVAL);
    }

    /**
     * Returns dyndns IP address
     *
     * @param newDns
     */

    public String getHostIP(String host) {

        if (host == null) {
            return "";
        }

        String strDyndnsIP = "";
        try {
            InetAddress myDyndnsIP = InetAddress.getByName(host);
            if (myDyndnsIP != null) {
                strDyndnsIP = myDyndnsIP.getHostAddress();
            }
        } catch (IllegalArgumentException ex) {
            logSevere("Can't get the host ip address" + ex.toString());
        } catch (UnknownHostException ex) {
            logSevere("Can't get the host ip address" + ex.toString());
        }
        return strDyndnsIP;
    }

    /**
     * Returns the internet address of this machine.
     *
     * @return the ip address or empty string if none is found
     */

    public String getIPviaHTTPCheckIP() {

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
            logFiner("Received '" + ipText + "' from " + dyndns);
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
    private static String filterIPs(String txt) {
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