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
package de.dal33t.powerfolder.clientserver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.AnonymousAccount;
import de.dal33t.powerfolder.security.FolderAdminPermission;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * Client to a server.
 * <p>
 * TODO Finalize Request <-> Response code.
 * <p>
 * Maybe FIXME: Check if MemberInfos with ID = "" cause problems. (Temporary for
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClient extends PFComponent {
    // The last used username and password.
    // Tries to re-login with these if re-connection happens
    private String username;
    private String password;

    private Member server;
    private AccountDetails accountDetails;
    private AccountService userService;
    private FolderService folderService;

    // Construction ***********************************************************

    public ServerClient(Controller controller, Member serverNode) {
        super(controller);
        Reject.ifNull(serverNode, "Server node is null");
        this.server = serverNode;
        initializeServiceStubs();
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        this.accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
    }

    // Basics *****************************************************************

    public void start() {
        getController().scheduleAndRepeat(new OnlineStorageConnectTask(),
            3L * 1000L, 1000L * 20);
    }

    /**
     * @return the server to connect to.
     */
    public Member getServer() {
        return server;
    }

    /**
     * @param node
     * @return true if the node is the server.
     */
    public boolean isServer(Member node) {
        return server.equals(node)
        // Compare by address, ID might be empty at start.
            || server.getReconnectAddress().equals(node.getReconnectAddress());
    }

    /**
     * @return if the server is connected
     */
    public boolean isConnected() {
        return server.isCompleteyConnected();
    }

    /**
     * @return the URL of the web access
     */
    public String getWebURL() {
        String host = ConfigurationEntry.SERVER_HOST.getValue(getController());
        if (!StringUtils.isBlank(host)) {
            int i = host.indexOf(":");
            if (i > 0) {
                host = host.substring(0, i);
            }
            return "http://" + host;
        }
        // Default
        return Constants.ONLINE_STORAGE_URL;
    }

    // Login ******************************************************************

    /**
     * @return true if the default account data has been set
     */
    public boolean isDefaultAccountSet() {
        // FIXME Use separate account stores for diffrent servers?
        return !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()))
            && !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
                .getValue(getController()));
    }

    /**
     * Logs into the server with the default username and password in config.
     * <p>
     * If the server is not connected and invalid account is returned and the
     * login data saved for auto-login on reconnect.
     * 
     * @return the identity with this username or <code>InvalidAccount</code>
     *         if login failed. NEVER returns <code>null</code>
     */
    public Account loginWithDefault() {
        return login(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()), ConfigurationEntry.WEBSERVICE_PASSWORD
            .getValue(getController()));
    }

    /**
     * Logs into the server and saves the identity as my login.
     * <p>
     * If the server is not connected and invalid account is returned and the
     * login data saved for auto-login on reconnect.
     * 
     * @param theUsername
     * @param thePassword
     * @return the identity with this username or <code>InvalidAccount</code>
     *         if login failed. NEVER returns <code>null</code>
     */
    public Account login(String theUsername, String thePassword) {
        username = theUsername;
        password = thePassword;
        if (!isConnected()) {
            accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
            return accountDetails.getAccount();
        }
        String salt = IdGenerator.makeId() + IdGenerator.makeId();
        String mix = salt + thePassword + salt;
        String passwordMD5;
        try {
            passwordMD5 = new String(Util.md5(mix.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found", e);
        }
        boolean loginOk = userService.login(theUsername, passwordMD5, salt);
        if (!loginOk) {
            log().warn("Login to server (" + theUsername + ") failed!");
            accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
            return accountDetails.getAccount();
        }
        AccountDetails newAccountDetails = userService.getAccountDetails();
        log().info(
            "Login to server (" + theUsername + ") result: " + accountDetails);
        if (newAccountDetails != null) {
            accountDetails = newAccountDetails;
        } else {
            accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
        }
        return accountDetails.getAccount();
    }

    /**
     * @return true if the last attempt to login to the online storage was ok.
     *         false if not or no login tried yet.
     */
    public boolean isLastLoginOK() {
        return accountDetails != null
            && (!(accountDetails.getAccount() instanceof AnonymousAccount));
    }

    /**
     * @return the user/account of the last login.
     */
    public Account getAccount() {
        return accountDetails != null ? accountDetails.getAccount() : null;
    }

    public AccountDetails getAccountDetails() {
        return accountDetails;
    }

    // Services ***************************************************************

    public AccountService getUserService() {
        return userService;
    }

    public FolderService getFolderService() {
        return folderService;
    }

    // Conviniece *************************************************************

    /**
     * @return the joined folders by the Server.
     */
    public List<Folder> getJoinedFolders() {
        List<Folder> mirroredFolders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (hasJoined(folder)) {
                mirroredFolders.add(folder);
            }
        }
        return mirroredFolders;
    }

    /**
     * @param folder
     *            the folder to check.
     * @return true if the server has joined the folder.
     */
    public boolean hasJoined(Folder folder) {
        return folder.hasMember(server);
    }

    /**
     * Syncs the folder memberships with the FolderAdminPermissions on the
     * server.
     */
    public void syncFolderRights() {
        Reject.ifFalse(isLastLoginOK(), "Last login not ok");
        // FolderInfo[] myFolders = getController().getFolderRepository()
        // .getJoinedFolderInfos();
        //
        // if (logWarn) {
        // log().warn(
        // "Granting admin permission on: " + Arrays.asList(myFolders));
        // }
        // getFolderService().grantAdmin(myFolders);

        log().warn("Rights: " + getAccount().getPermissions().size());
        // TODO Also get READ/WRITE permission folder
        Collection<FolderInfo> foInfos = FolderAdminPermission
            .filter(getAccount());
        log().warn("Rights on: " + foInfos);
        for (FolderInfo foInfo : foInfos) {
            log().warn("Checking: " + foInfo);
            if (getController().getFolderRepository().hasJoinedFolder(foInfo)) {
                continue;
            }
            FolderSettings settings = new FolderSettings(new File("."),
                SyncProfile.AUTOMATIC_SYNCHRONIZATION, true, true, true, false);
            log().warn("Adding as preview: " + foInfo);
            getController().getFolderRepository().createPreviewFolder(foInfo,
                settings);
        }
    }

    // Internal ***************************************************************

    private void initializeServiceStubs() {
        userService = ServiceProvider.createRemoteStub(getController(),
            AccountService.class, server);
        folderService = ServiceProvider.createRemoteStub(getController(),
            FolderService.class, server);
    }

    // General ****************************************************************

    public String toString() {
        return "ServerClient to " + (server != null ? server : "n/a");
    }

    // Inner classes **********************************************************

    /**
     * This listener violates the rule "Listener/Event usage". Reason: Even when
     * a ServerClient is a true core-component there might be multiple
     * ClientServer objects that dynamically change.
     * <p>
     * http://dev.powerfolder.com/projects/powerfolder/wiki/GeneralDevelopRules
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeConnected(NodeManagerEvent e) {
            if (isServer(e.getNode())) {

                if (StringUtils.isEmpty(server.getId())) {

                    // Got connect to server! Take his ID and name.
                    Member oldServer = server;
                    server = e.getNode();
                    // Remove old temporary server entry without ID.
                    getController().getNodeManager().removeNode(oldServer);
                    // Re-initalize the service stubs on new server
                    // node.
                    initializeServiceStubs();

                    log().debug("Got connect to server: " + server);
                }

                if (username != null) {
                    login(username, password);
                }
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (isServer(e.getNode())) {
                // Invalidate account.
                // TODO: Why no cache until reconnect?
                // myAccount = null;
            }
        }

        public void friendAdded(NodeManagerEvent e) {
            // NOP
        }

        public void friendRemoved(NodeManagerEvent e) {
            // NOP
        }

        public void nodeAdded(NodeManagerEvent e) {
            // NOP
        }

        public void nodeRemoved(NodeManagerEvent e) {
            // NOP
        }

        public void settingsChanged(NodeManagerEvent e) {
            // NOP
        }

        public void startStop(NodeManagerEvent e) {
            // NOP
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    private class OnlineStorageConnectTask extends TimerTask {
        private ReentrantLock alreadyConnectingLock = new ReentrantLock();

        @Override
        public void run() {
            if (isConnected()) {
                return;
            }
            if (server.isMySelf()) {
                // Don't connect to myself
                return;
            }
            if (getController().isLanOnly() && !server.isOnLAN()) {
                log().warn(
                    "NOT connecting to server: " + server
                        + ". Reason: Not on LAN");
                return;
            }
            if (!getController().getNodeManager().isStarted()) {
                return;
            }
            // if (!ConfigurationEntry.AUTO_CONNECT
            // .getValueBoolean(getController()))
            // {
            // return;
            // }
            if (alreadyConnectingLock.isLocked()) {
                // Already triing
                return;
            }

            Runnable connector = new Runnable() {
                public void run() {
                    try {
                        if (isConnected()) {
                            return;
                        }
                        if (!alreadyConnectingLock.tryLock()) {
                            // Already triing
                            return;
                        }
                        if (!StringUtils.isEmpty(server.getId())) {
                            log().warn(
                                "Triing to reconnect to Server (" + server
                                    + ")");
                            // With ID directly connect
                            if (!server.isReconnecting()) {
                                server.reconnect();
                            } else {
                                log().debug(
                                    "Not reconnecting. Already triing to connect to "
                                        + server);
                            }
                        } else {
                            log().warn(
                                "Triing to connect to Server by address ("
                                    + server + ")");
                            // Get "full" Member with ID. after direct connect
                            // to IP.
                            ConnectionHandler conHan = getController()
                                .getIOProvider().getConnectionHandlerFactory()
                                .tryToConnect(server.getReconnectAddress());
                            Member oldServer = server;
                            server = getController().getNodeManager()
                                .acceptConnection(conHan);
                            // Remove old temporary server entry without ID.
                            getController().getNodeManager().removeNode(
                                oldServer);
                            // Re-initalize the service stubs on new server
                            // node.
                            initializeServiceStubs();
                            log().info("Got connect to server: " + server);
                        }
                    } catch (ConnectionException e) {
                        log().warn("Unable to connect to " + ServerClient.this,
                            e);
                    } finally {
                        alreadyConnectingLock.unlock();
                    }
                }
            };
            getController().getIOProvider().startIO(connector);
        }
    }
}
