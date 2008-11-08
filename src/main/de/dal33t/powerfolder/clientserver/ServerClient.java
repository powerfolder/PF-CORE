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

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.AnonymousAccount;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;

/**
 * Client to a server.
 * <p>
 * Maybe FIXME: Check if MemberInfos with ID = "" cause problems. (Temporary for
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClient extends PFComponent {
    private static final String PREFS_PREFIX = "server";
    private static final String MEMBER_ID_TEMP_PREFIX = "TEMP_IDENTITY_";

    // The last used username and password.
    // Tries to re-login with these if re-connection happens
    private String username;
    private String password;

    private Member server;
    /**
     * If this client should connect to the server where his folders are hosted
     * on.
     */
    private boolean allowServerChange;
    private AccountDetails accountDetails;
    private AccountService userService;
    private FolderService folderService;
    private PublicKeyService publicKeyService;

    private ServerClientListener listenerSupport;

    /**
     * For test
     */
    private String webURL;

    // Construction ***********************************************************

    /**
     * Constructs a server client with a given member info.
     * 
     * @param controller
     * @param serverNode
     */
    public ServerClient(Controller controller, Member serverNode) {
        super(controller);
        init(serverNode, true);
    }

    /**
     * Constructs a server client to a given host.
     * 
     * @param controller
     * @param host
     * @param allowServerChange
     */
    public ServerClient(Controller controller, String host,
        boolean allowServerChange)
    {
        super(controller);
        init(createTempServerNode(host), allowServerChange);
    }

    private void init(Member serverNode, boolean serverChange) {
        Reject.ifNull(serverNode, "Server node is null");
        this.listenerSupport = ListenerSupportFactory
            .createListenerSupport(ServerClientListener.class);
        setNewServerNode(serverNode);
        // Allowed by default
        this.allowServerChange = serverChange;
        setAnonAccount();
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    private Member createTempServerNode(String host) {
        MemberInfo serverInfo = new MemberInfo(Translation
            .getTranslation("online_storage.connecting"), MEMBER_ID_TEMP_PREFIX
            + "|" + IdGenerator.makeId());
        serverInfo.setConnectAddress(Util.parseConnectionString(host));
        logInfo("Using server from config: " + serverInfo + ", ID: "
            + serverInfo.id);
        // Avoid adding temporary nodes to nodemanager
        return new Member(getController(), serverInfo);
        // return serverInfo.getNode(getController(), true);
    }

    private boolean isTempServerNode() {
        return server.getId().startsWith(MEMBER_ID_TEMP_PREFIX);
    }

    private boolean isRemindPassword() {
        return PreferencesEntry.SERVER_REMIND_PASSWORD
            .getValueBoolean(getController());
    }

    // Basics *****************************************************************

    public void start() {
        if (getController().isLanOnly() && !server.isOnLAN()) {
            logFine("Not connecting to server: " + server
                + ". Reason: Server not on LAN");
        }
        getController().scheduleAndRepeat(new OnlineStorageConnectTask(),
            3L * 1000L, 1000L * 20);
        // Wait 10 seconds at start
        getController().scheduleAndRepeat(new HostingServerRetriever(),
            10L * 1000L, 1000L * Constants.HOSTING_FOLDERS_REQUEST_INTERVAL);
        // Don't start, not really required?
        // getController().scheduleAndRepeat(new AccountRefresh(), 1000L * 30,
        // 1000L * 30);
    }

    /**
     * Answers if the node is a temporary node info for a server. It does not
     * contains a valid id, but a hostname/port.
     * 
     * @param node
     * @return true if the node is a temporary node info.
     */
    public static boolean isTempServerNode(MemberInfo node) {
        return node.id.startsWith(MEMBER_ID_TEMP_PREFIX);
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
            || (isTempServerNode() && server.getReconnectAddress().equals(
                node.getReconnectAddress()));
    }

    /**
     * @return if the server is connected
     */
    public boolean isConnected() {
        return server.isCompleteyConnected();
    }

    /**
     * @return the URL of the web access to the server (cluster).
     */
    public String getWebURL() {
        if (webURL != null) {
            return webURL;
        }
        if (accountDetails != null
            && accountDetails.getAccount() != null
            && accountDetails.getAccount().getServer() != null
            && !StringUtils.isBlank(accountDetails.getAccount().getServer()
                .getWebUrl()))
        {
            return accountDetails.getAccount().getServer().getWebUrl();
        }

        // FIXME: Does not work, http port does not get considered!
        // Fallback
        // String host =
        // ConfigurationEntry.SERVER_HOST.getValue(getController());
        // if (!StringUtils.isBlank(host)) {
        // int i = host.indexOf(":");
        // if (i > 0) {
        // host = host.substring(0, i);
        // }
        // return "http://" + host;
        // }

        // Default fallback
        return Constants.ONLINE_STORAGE_URL;
    }

    /**
     * @param webURL
     *            the WEB URL to use for basic web services
     */
    public void setWebURL(String webURL) {
        this.webURL = webURL;
    }

    /**
     * Convenience method for getting register URL
     * 
     * @return the registration URL for this server.
     */
    public String getRegisterURL() {
        return getWebURL() + "/register";
    }

    /**
     * Convenience method for getting activation URL
     * 
     * @return the activation URL for this server.
     */
    public String getActivationURL() {
        return getWebURL() + "/activate";
    }

    public boolean isAllowServerChange() {
        return allowServerChange;
    }

    // Login ******************************************************************

    /**
     * @return true if we know last login data. uses default account setting as
     *         fallback
     */
    public boolean isLastLoginKnown() {
        return getController().getPreferences().get(
            PREFS_PREFIX + "." + server.getIP() + ".username", null) != null
            || isDefaultAccountSet();
    }

    /**
     * Tries to logs in with the last know username/password combination for
     * this server.uses default account setting as fallback
     * 
     * @return the identity with this username or <code>InvalidAccount</code>
     *         if login failed. NEVER returns <code>null</code>
     */
    public Account loginWithLastKnown() {
        String un = getController().getPreferences().get(
            PREFS_PREFIX + "." + server.getIP() + ".username", null);
        String pw = getController().getPreferences().get(
            PREFS_PREFIX + "." + server.getIP() + ".info2", null);
        if (!StringUtils.isBlank(pw)) {
            try {
                pw = new String(Base64.decode(pw), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Fallback (TRAC #1291)
            pw = getController().getPreferences().get(
                PREFS_PREFIX + "." + server.getIP() + ".info", null);
        }

        if (!StringUtils.isBlank(un)) {
            return login(un, pw);
        }
        // Fallback
        if (isDefaultAccountSet()) {
            return loginWithDefault();
        }
        // Failed!
        return null;
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
        saveLastKnowLogin();
        if (!isConnected()) {
            setAnonAccount();
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
            logWarning("Login to server server " + server.getReconnectAddress()
                + " (user " + theUsername + ") failed!");
            setAnonAccount();
            return accountDetails.getAccount();
        }
        AccountDetails newAccountDetails = userService.getAccountDetails();
        logInfo("Login to server " + server.getReconnectAddress() + " (user "
            + theUsername + ") result: " + accountDetails);
        if (newAccountDetails != null) {
            accountDetails = newAccountDetails;

            // Fire login success
            fireLogin(accountDetails);

            // Possible switch to new server
            ServerInfo targetServer = accountDetails.getAccount().getServer();
            if (targetServer != null && allowServerChange) {
                // Not hosted on the server we just have logged into.
                boolean changeServer = !server.getInfo().equals(
                    targetServer.getNode());
                if (changeServer) {
                    changeToServer(targetServer);
                }
            }
        } else {
            setAnonAccount();
        }
        return accountDetails.getAccount();
    }

    /**
     * @return true if the last attempt to login to the online storage was ok.
     *         false if not or no login tried yet.
     */
    public boolean isLastLoginOK() {
        return getAccount() != null && getAccount().isValid();
    }

    /**
     * @return the username that is set for login.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password that is set for login.
     */
    public String getPassword() {
        return password;
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

    /**
     * Re-loads the account details from server. Should be done if it's likely
     * that currently logged in account has changed.
     * 
     * @return the new account details
     */
    public AccountDetails refreshAccountDetails() {
        AccountDetails newDetails = userService.getAccountDetails();
        if (newDetails != null) {
            accountDetails = newDetails;
            fireAccountUpdates(accountDetails);
        } else {
            setAnonAccount();
        }
        return accountDetails;
    }

    // Services ***************************************************************

    public AccountService getAccountService() {
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
        // logWarning(
        // "Granting admin permission on: " + Arrays.asList(myFolders));
        // }
        // getFolderService().grantAdmin(myFolders);

        logWarning("Rights: " + getAccount().getPermissions().size());
        // TODO Also get READ/WRITE permission folder
        // Collection<FolderInfo> foInfos = FolderAdminPermission
        // .filter(getAccount());
        // logWarning("Rights on: " + foInfos);
        // for (FolderInfo foInfo : foInfos) {
        // logWarning("Checking: " + foInfo);
        // if (getController().getFolderRepository().hasJoinedFolder(foInfo)) {
        // continue;
        // }
        // FolderSettings settings = new FolderSettings(new File("."),
        // SyncProfile.AUTOMATIC_SYNCHRONIZATION, true, true, true, false);
        // logWarning("Adding as preview: " + foInfo);
        // getController().getFolderRepository().createPreviewFolder(foInfo,
        // settings);
        // }
    }

    /**
     * Tries to connect hosting servers of our locally joined folders. Call this
     * when it is expected, that any of the locally joined folders is hosted on
     * another server. This method does NOT block, it instead schedules a
     * background task to retrieve and connect those servers.
     */
    public void connectHostingServers() {
        if (!isConnected()) {
            return;
        }
        if (!isLastLoginOK()) {
            return;
        }
        if (isFiner()) {
            logFiner("Connecting to hosting servers");
        }
        Runnable retriever = new Runnable() {
            public void run() {
                FolderInfo[] folders = getController().getFolderRepository()
                    .getJoinedFolderInfos();
                Collection<MemberInfo> servers = getFolderService()
                    .getHostingServers(folders);
                logWarning("Got " + servers.size() + " servers for our "
                    + folders.length + " folders: " + servers);
                for (MemberInfo serverMInfo : servers) {
                    Member hostingServer = serverMInfo.getNode(getController(),
                        true);
                    if (hostingServer.isCompleteyConnected()
                        || hostingServer.isReconnecting()
                        || hostingServer.equals(server))
                    {
                        // Already connected / reconnecting
                        continue;
                    }
                    // Connect now
                    hostingServer.setServer(true);
                    hostingServer.markForImmediateConnect();
                }
            }
        };
        getController().getIOProvider().startIO(retriever);
    }

    // Event handling ********************************************************

    public void addListener(ServerClientListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeListener(ServerClientListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    // Internal ***************************************************************

    private void setNewServerNode(Member newServerNode) {
        server = newServerNode;
        server.setServer(true);
        // Re-initalize the service stubs on new server node.
        initializeServiceStubs();
    }

    private void initializeServiceStubs() {
        userService = ServiceProvider.createRemoteStub(getController(),
            AccountService.class, server);
        folderService = ServiceProvider.createRemoteStub(getController(),
            FolderService.class, server);
        publicKeyService = ServiceProvider.createRemoteStub(getController(),
            PublicKeyService.class, server);
    }

    private void setAnonAccount() {
        accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
        fireLogin(accountDetails);
    }

    private void saveLastKnowLogin() {
        if (!StringUtils.isBlank(username)) {
            getController().getPreferences().put(
                PREFS_PREFIX + "." + server.getIP() + ".username", username);
        }

        if (isRemindPassword() && !StringUtils.isBlank(password)) {
            try {
                getController().getPreferences().put(
                    PREFS_PREFIX + "." + server.getIP() + ".info2",
                    Base64.encodeBytes(password.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 not found", e);
            }
        } else {
            getController().getPreferences().remove(
                PREFS_PREFIX + "." + server.getIP() + ".info2");
        }

        // Clear plain text password (TRAC #1291)
        getController().getPreferences().remove(
            PREFS_PREFIX + "." + server.getIP() + ".info");
    }

    /**
     * For backward compatibility
     * 
     * @return true if the default account data has been set
     */
    private boolean isDefaultAccountSet() {
        return !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()))
            && !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
                .getValue(getController()));
    }

    /**
     * For backward compatibility
     * <p>
     * Logs into the server with the default username and password in config.
     * <p>
     * If the server is not connected and invalid account is returned and the
     * login data saved for auto-login on reconnect.
     * 
     * @return the identity with this username or <code>InvalidAccount</code>
     *         if login failed. NEVER returns <code>null</code>
     */
    private Account loginWithDefault() {
        Account a = login(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()), ConfigurationEntry.WEBSERVICE_PASSWORD
            .getValue(getController()));
        // At least the last know has been written. remove username & password
        // from config.
        ConfigurationEntry.WEBSERVICE_USERNAME.removeValue(getController());
        ConfigurationEntry.WEBSERVICE_PASSWORD.removeValue(getController());
        getController().saveConfig();
        return a;
    }

    private void changeToServer(ServerInfo targetServer) {
        logWarning("Changeing server to " + targetServer.getNode());

        // Add key of new server to keystore.
        if (Util.getPublicKey(getController(), targetServer.getNode()) == null)
        {
            PublicKey serverKey = publicKeyService.getPublicKey(targetServer
                .getNode());
            if (serverKey != null) {
                logWarning("Retrieved new key for server "
                    + targetServer.getNode() + ". " + serverKey);
                Util.addNodeToKeyStore(getController(), targetServer.getNode(),
                    serverKey);
            }
        }

        // Remind new server for next connect.
        if (targetServer.getNode().getConnectAddress() != null) {
            ConfigurationEntry.SERVER_HOST.setValue(getController(),
                targetServer.getNode().getConnectAddress().getHostName() + ':'
                    + targetServer.getNode().getConnectAddress().getPort());
            getController().saveConfig();
        }

        // Now actually switch to new server.
        setNewServerNode(targetServer.getNode().getNode(getController(), true));
        // Attempt to login. At least remind login for real connect.
        login(username, password);
        if (!isConnected()) {
            // Mark new server for connect
            server.markForImmediateConnect();
        }
    }

    private void fireLogin(AccountDetails details) {
        listenerSupport.login(new ServerClientEvent(this, details));
    }

    private void fireAccountUpdates(AccountDetails details) {
        listenerSupport.accountUpdated(new ServerClientEvent(this, details));
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
            // logWarning("Is server " + e.getNode() + "? " +
            // isServer(e.getNode()));
            if (isServer(e.getNode())) {
                // Our server member instance is a temporary one. Lets get real.
                if (isTempServerNode()) {
                    // Got connect to server! Take his ID and name.
                    Member oldServer = server;
                    setNewServerNode(e.getNode());
                    // Remove old temporary server entry without ID.
                    getController().getNodeManager().removeNode(oldServer);
                    logFine("Got connect to server: " + server);
                }

                if (username != null) {
                    login(username, password);
                }
                listenerSupport.serverConnected(new ServerClientEvent(
                    ServerClient.this));
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (isServer(e.getNode())) {
                // Invalidate account.
                // TODO: Why no cache until reconnect?
                // myAccount = null;
                listenerSupport.serverDisconnected(new ServerClientEvent(
                    ServerClient.this));
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

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    private class OnlineStorageConnectTask extends TimerTask {
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
                logFiner("NOT connecting to server: " + server
                    + ". Reason: Not on LAN");
                return;
            }
            if (!getController().getNodeManager().isStarted()) {
                return;
            }
            if (server.isReconnecting() || server.isConnected()) {
                return;
            }
            // Try to connect
            server.markForImmediateConnect();
        }
    }

    /**
     * Task to retrieve hosting Online Storage servers which host my files.
     */
    private class HostingServerRetriever extends TimerTask {
        @Override
        public void run() {
            connectHostingServers();
        }
    }

    // private class AccountRefresh extends TimerTask {
    // @Override
    // public void run() {
    // if (isConnected()) {
    // return;
    // }
    // if (server.isMySelf()) {
    // // Don't connect to myself
    // return;
    // }
    // if (isLastLoginOK()) {
    // Runnable refresher = new Runnable() {
    // public void run() {
    // refreshAccountDetails();
    // }
    // };
    // getController().getIOProvider().startIO(refresher);
    // }
    // }
    // }

}
