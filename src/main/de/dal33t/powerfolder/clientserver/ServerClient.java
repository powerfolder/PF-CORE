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

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.AnonymousAccount;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.update.Updater;

/**
 * Client to a server.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClient extends PFComponent {
    private static final String PREFS_PREFIX = "server";
    private static final String MEMBER_ID_TEMP_PREFIX = "TEMP_IDENTITY_";

    // The last used username and password.
    // Tries to re-login with these if re-connection happens
    private String username;
    // TODO Convert to char[] for security reasons.
    private String password;

    private Member server;
    private String webURL;

    /**
     * If this client should connect to the server where his folders are hosted
     * on.
     */
    private boolean allowServerChange;
    /**
     * Update the config with new HOST/ID infos if retrieved from server.
     */
    private boolean updateConfig;

    /**
     * Log that is kept to synchronize calls to login
     */
    private final Object loginLock = new Object();

    private AccountDetails accountDetails;

    private ServiceProvider serviceProvider;
    private SecurityService securityService;
    private AccountService userService;
    private FolderService folderService;
    private PublicKeyService publicKeyService;

    private ServerClientListener listenerSupport;

    // Construction ***********************************************************

    /**
     * Constructs a server client with the defaults from the config. allows
     * server change.
     * 
     * @param controller
     */
    public ServerClient(Controller controller) {
        this(controller, ConfigurationEntry.SERVER_NAME.getValue(controller),
            ConfigurationEntry.SERVER_HOST.getValue(controller),
            ConfigurationEntry.SERVER_NODEID.getValue(controller),
            ConfigurationEntry.SERVER_WEB_URL.getValue(controller), true,
            ConfigurationEntry.SERVER_CONFIG_UPDATE.getValueBoolean(controller));
    }

    /**
     * Constructs a server client with the defaults from the config.
     * 
     * @param controller
     * @param name
     * @param host
     * @param nodeId
     * @param webURL
     * @param allowServerChange
     * @param updateConfig
     */
    public ServerClient(Controller controller, String name, String host,
        String nodeId, String webURL, boolean allowServerChange,
        boolean updateConfig)
    {
        super(controller);

        this.allowServerChange = allowServerChange;
        this.updateConfig = updateConfig;
        this.webURL = !StringUtils.isBlank(webURL) ? Util
            .removeLastSlashFromURI(webURL) : null;
        this.serviceProvider = new RemoteServiceProvider();

        // Custom server
        String theName = !StringUtils.isBlank(name) ? name : Translation
            .getTranslation("online_storage.connecting");
        boolean temporaryNode = StringUtils.isBlank(nodeId);
        String theNodeId = !temporaryNode ? nodeId : MEMBER_ID_TEMP_PREFIX
            + '|' + IdGenerator.makeId();
        Member theNode = getController().getNodeManager().getNode(theNodeId);
        if (theNode == null) {
            String networkId = getController().getNodeManager().getNetworkId();
            MemberInfo serverNode = new MemberInfo(theName, theNodeId,
                networkId);
            if (temporaryNode) {
                // Temporary node. Don't add to nodemanager
                theNode = new Member(getController(), serverNode);
            } else {
                theNode = serverNode.getNode(getController(), true);
            }
        }
        if (!StringUtils.isBlank(host)) {
            theNode.getInfo().setConnectAddress(
                Util.parseConnectionString(host));
        }

        if (theNode.getReconnectAddress() == null) {
            logSevere("Got server without reconnect address: " + theNode);
        }
        logInfo("Using server from config: " + theNode + ", ID: " + theNodeId);
        init(theNode, allowServerChange);
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

    private boolean isRememberPassword() {
        return PreferencesEntry.SERVER_REMEMBER_PASSWORD
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
    public static boolean isTempServerNode(Member node) {
        return node.getId().startsWith(MEMBER_ID_TEMP_PREFIX);
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
        if (server.equals(node)) {
            return true;
        }
        // if (isTempServerNode()
        // && (node.getId().contains("WEBSER") || node.getId().contains(
        // "RELAY")))
        // {
        // logWarning(
        // "isServer: node: "
        // + node.getReconnectAddress()
        // + ". have: "
        // + server.getReconnectAddress()
        // + ". equals? "
        // + server.getReconnectAddress().equals(
        // node.getReconnectAddress()));
        // }
        return isTempServerNode(server)
            && server.getReconnectAddress().equals(node.getReconnectAddress());
    }

    /**
     * Sets/Changes the server.
     * 
     * @param serverNode
     * @param serverChange
     */
    public void setServer(Member serverNode, boolean serverChange) {
        setServer(serverNode, serverChange, new RemoteServiceProvider());
    }

    /**
     * Sets/Changes the server.
     * 
     * @param serverNode
     * @param serverChange
     * @param serviceProvider
     *            the service provider to use.
     */
    public void setServer(Member serverNode, boolean serverChange,
        ServiceProvider serviceProvider)
    {
        Reject.ifNull(serverNode, "Server node is null");
        Reject.ifNull(serviceProvider, "ServiceProvider is null");
        this.serviceProvider = serviceProvider;
        setNewServerNode(serverNode);
        this.allowServerChange = serverChange;
        loginWithLastKnown();
        if (!isConnected()) {
            getServer().markForImmediateConnect();
        }
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    /**
     * @return if the server is connected
     */
    public boolean isConnected() {
        return server.isMySelf() || server.isCompletelyConnected();
    }

    /**
     * @return the URL of the web access to the server (cluster).
     */
    public String getWebURL() {
        if (!StringUtils.isBlank(webURL)) {
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

        // No web url.
        return null;
    }

    /**
     * @return true if the connected server offers a web interface.
     */
    public boolean hasWebURL() {
        return getWebURL() != null;
    }

    /**
     * @return true if client supports register on registerURL.
     */
    public boolean supportsWebRegistration() {
        return ConfigurationEntry.SERVER_REGISTER_ENABLED
            .getValueBoolean(getController());
    }

    /**
     * Convenience method for getting login URL with preset username and
     * password if possible
     * 
     * @return the login URL
     */
    public String getLoginURLWithCredentials() {
        if (!hasWebURL()) {
            return null;
        }
        String url = getWebURL() + Constants.LOGIN_URI;
        if (StringUtils.isNotBlank(getUsername())) {
            url += "?";
            url += Constants.LOGIN_PARAM_USERNAME;
            url += "=";
            url += Util.endcodeForURL(getUsername());
            if (StringUtils.isNotBlank(getPassword())) {
                String salt = IdGenerator.makeId() + IdGenerator.makeId();
                String mix = salt + getPassword().trim() + salt;
                byte[] passwordMD5 = Util.md5(mix.getBytes(Convert.UTF8));
                url += "&";
                url += Constants.LOGIN_PARAM_PASSWORD_MD5;
                url += "=";
                url += Util.endcodeForURL(Base64.encodeBytes(passwordMD5));
                url += "&";
                url += Constants.LOGIN_PARAM_SALT;
                url += "=";
                url += Util.endcodeForURL(Base64.encodeString(salt));
            }
        }
        return url;
    }

    /**
     * Convenience method for getting login URL with preset username if possible
     * 
     * @return the registration URL for this server.
     */
    public String getRecoverPasswordURL() {
        if (!hasWebURL()) {
            return null;
        }
        String url = getWebURL() + Constants.LOGIN_URI;
        if (StringUtils.isNotBlank(getUsername())) {
            url += "?";
            url += Constants.LOGIN_PARAM_USERNAME;
            url += "=";
            url += getUsername();
        }
        return url;
    }

    /**
     * Convenience method for getting register URL
     * 
     * @return the registration URL for this server.
     */
    public String getRegisterURL() {
        if (!supportsWebRegistration()) {
            return null;
        }
        if (!hasWebURL()) {
            return null;
        }
        return getWebURL() + "/register";
    }

    /**
     * Convenience method for getting activation URL
     * 
     * @return the activation URL for this server.
     */
    public String getActivationURL() {
        if (!hasWebURL()) {
            return null;
        }
        return getWebURL() + "/activate";
    }

    /**
     * @return update settings to check and download new program client
     *         versions.
     */
    public Updater.UpdateSetting createUpdateSettings() {
        Updater.UpdateSetting settings = new Updater.UpdateSetting();

        if (!hasWebURL()) {
            logSevere("Unable to created update settings, no web URL for server found. "
                + getServer() + " using defaults.");
            return settings;
        }

        settings.versionCheckURL = getWebURL()
            + "/client_deployment/PowerFolderPro_LatestVersion.txt";
        settings.downloadLinkInfoURL = null;
        settings.releaseExeURL = getWebURL()
            + "/client_deployment/PowerFolder_Latest_Win32_Installer.exe";

        return settings;
    }

    // Login ******************************************************************

    /**
     * @return true if we know last login data. uses default account setting as
     *         fallback
     */
    public boolean isLastLoginKnown() {
        return getController().getPreferences().get(
            PREFS_PREFIX + '.' + server.getIP() + ".username", null) != null;
    }

    /**
     * Tries to logs in with the last know username/password combination for
     * this server.uses default account setting as fallback
     * 
     * @return the identity with this username or <code>InvalidAccount</code> if
     *         login failed. NEVER returns <code>null</code>
     */
    public Account loginWithLastKnown() {
        String un = getController().getPreferences().get(
            PREFS_PREFIX + '.' + server.getIP() + ".username", null);
        String pw = getController().getPreferences().get(
            PREFS_PREFIX + '.' + server.getIP() + ".info2", null);
        if (StringUtils.isNotBlank(pw)) {
            pw = new String(Base64.decode(pw), Convert.UTF8);
        } else {
            // Fallback (TRAC #1291)
            pw = getController().getPreferences().get(
                PREFS_PREFIX + '.' + server.getIP() + ".info", null);
        }

        if (!StringUtils.isBlank(un) && !StringUtils.isBlank(pw)) {
            return login(un, pw);
        }
        // Failed!
        return null;
    }

    /**
     * Log out of online storage.
     */
    public void logoff() {
        username = null;
        password = null;
        saveLastKnowLogin();
        setAnonAccount();
        fireLogin(accountDetails);
    }

    /**
     * Logs into the server and saves the identity as my login.
     * <p>
     * If the server is not connected and invalid account is returned and the
     * login data saved for auto-login on reconnect.
     * 
     * @param theUsername
     * @param thePassword
     * @return the identity with this username or <code>InvalidAccount</code> if
     *         login failed. NEVER returns <code>null</code>
     */
    public Account login(String theUsername, String thePassword) {
        synchronized (loginLock) {
            password = thePassword != null ? thePassword.trim() : null;
            String salt = IdGenerator.makeId() + IdGenerator.makeId();
            String mix = salt + password + salt;
            String passwordMD5 = new String(Util
                .md5(mix.getBytes(Convert.UTF8)), Convert.UTF8);
            return login(theUsername, passwordMD5, salt);
        }
    }

    /**
     * Logs into the server and saves the identity as my login.
     * <p>
     * If the server is not connected and invalid account is returned and the
     * login data saved for auto-login on reconnect.
     * 
     * @param theUsername
     * @param thePasswordMD5
     * @param salt
     * @return the identity with this username or <code>InvalidAccount</code> if
     *         login failed. NEVER returns <code>null</code>
     */
    public Account login(String theUsername, String thePasswordMD5, String salt)
    {
        synchronized (loginLock) {
            username = theUsername;
            // TODO Save MD5/Salted password
            saveLastKnowLogin();
            if (!isConnected()) {
                setAnonAccount();
                fireLogin(accountDetails);
                return accountDetails.getAccount();
            }
            boolean loginOk = securityService.login(theUsername,
                thePasswordMD5, salt);
            if (!loginOk) {
                logWarning("Login to server server "
                    + server.getReconnectAddress() + " (user " + theUsername
                    + ") failed!");
                setAnonAccount();
                fireLogin(accountDetails);
                return accountDetails.getAccount();
            }
            AccountDetails newAccountDetails = securityService
                .getAccountDetails();
            logFine("Login to server " + server.getReconnectAddress()
                + " (user " + theUsername + ") result: " + newAccountDetails);
            if (newAccountDetails != null) {
                accountDetails = newAccountDetails;

                if (updateConfig) {
                    boolean configChanged;
                    if (accountDetails.getAccount().getServer() != null) {
                        configChanged = setServerWebURLInConfig(accountDetails
                            .getAccount().getServer().getWebUrl());
                        configChanged = setServerHTTPTunnelURLInConfig(accountDetails
                            .getAccount().getServer().getHTTPTunnelUrl())
                            || configChanged;
                    } else {
                        configChanged = setServerWebURLInConfig(null);
                        configChanged = setServerHTTPTunnelURLInConfig(null)
                            || configChanged;
                    }
                    if (configChanged) {
                        getController().saveConfig();
                    }
                }

                // Fire login success
                fireLogin(accountDetails);
                updateFriendsList(accountDetails.getAccount());

                // Possible switch to new server
                ServerInfo targetServer = accountDetails.getAccount()
                    .getServer();
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
                fireLogin(accountDetails);
            }
            return accountDetails.getAccount();
        }
    }

    /**
     * @return true if the last attempt to login to the online storage was ok.
     *         false if not or no login tried yet.
     */
    public boolean isLoggedIn() {
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
     * @return the {@link AccountInfo} for the logged in account. or null if not
     *         logged in.
     */
    public AccountInfo getAccountInfo() {
        Account a = getAccount();
        return a != null && a.isValid() ? a.createInfo() : null;
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
        AccountDetails newDetails = securityService.getAccountDetails();
        if (newDetails != null) {
            accountDetails = newDetails;
            fireAccountUpdates(accountDetails);
            updateFriendsList(accountDetails.getAccount());
        } else {
            setAnonAccount();
            fireLogin(accountDetails);
        }
        if (isFine()) {
            logFine("Refreshed " + accountDetails);
        }
        return accountDetails;
    }

    private void updateFriendsList(Account a) {
        for (MemberInfo nodeInfo : a.getComputers()) {
            Member node = nodeInfo.getNode(getController(), true);
            if (!node.isFriend()) {
                node.setFriend(true, null);
            }
        }
    }

    // Services ***************************************************************

    public SecurityService getSecurityService() {
        return securityService;
    }

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
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            if (hasJoined(folder)) {
                mirroredFolders.add(folder);
            }
        }
        return mirroredFolders;
    }

    /**
     * @return a list of folder infos that are available on this account. These
     *         folder may or may not be backed up by the Online Storage/Server.
     */
    public Collection<FolderInfo> getAccountFolders() {
        return getAccount().getFolders();
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
     * Tries to connect hosting servers of our locally joined folders. Call this
     * when it is expected, that any of the locally joined folders is hosted on
     * another server. This method does NOT block, it instead schedules a
     * background task to retrieve and connect those servers.
     */
    public void connectHostingServers() {
        if (!isConnected()) {
            return;
        }
        if (!isLoggedIn()) {
            return;
        }
        if (isFiner()) {
            logFiner("Connecting to hosting servers");
        }
        Runnable retriever = new Runnable() {
            public void run() {
                Collection<FolderInfo> infos = getController()
                    .getFolderRepository().getJoinedFolderInfos();
                FolderInfo[] folders = infos.toArray(new FolderInfo[infos
                    .size()]);
                Collection<MemberInfo> servers = getFolderService()
                    .getHostingServers(folders);
                logFine("Got " + servers.size() + " servers for our "
                    + folders.length + " folders: " + servers);
                for (MemberInfo serverMInfo : servers) {
                    Member hostingServer = serverMInfo.getNode(getController(),
                        true);
                    if (hostingServer.isConnected()
                        || hostingServer.isConnecting()
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

    /**
     * Saves the infos of the server into the config properties. Does not save
     * the config file.
     * 
     * @param newServer
     */
    public void setServerInConfig(MemberInfo newServer) {
        Reject.ifNull(newServer, "Server is null");

        ConfigurationEntry.SERVER_NAME
            .setValue(getController(), newServer.nick);
        // This probably causes a reverse lookup of the IP.
        String serverHost = newServer.getConnectAddress().getHostName();
        if (newServer.getConnectAddress().getPort() != ConnectionListener.DEFAULT_PORT)
        {
            serverHost += ':';
            serverHost += newServer.getConnectAddress().getPort();
        }
        ConfigurationEntry.SERVER_HOST.setValue(getController(), serverHost);
        if (!isTempServerNode(newServer)) {
            ConfigurationEntry.SERVER_NODEID.setValue(getController(),
                newServer.id);
        } else {
            ConfigurationEntry.SERVER_NODEID.removeValue(getController());
        }
    }

    public boolean setServerWebURLInConfig(String newWebUrl) {
        this.webURL = newWebUrl;
        String oldWebUrl = ConfigurationEntry.SERVER_WEB_URL
            .getValue(getController());
        if (Util.equals(oldWebUrl, newWebUrl)) {
            return false;
        }
        // Currently not supported from config
        if (StringUtils.isBlank(newWebUrl)) {
            ConfigurationEntry.SERVER_WEB_URL.removeValue(getController());
        } else {
            ConfigurationEntry.SERVER_WEB_URL.setValue(getController(),
                newWebUrl);
        }
        return true;
    }

    private boolean setServerHTTPTunnelURLInConfig(String newTunnelURL) {
        logWarning("New tunnel URL");
        String oldUrl = ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL
            .getValue(getController());
        if (Util.equals(oldUrl, newTunnelURL)) {
            return false;
        }
        // Currently not supported from config
        if (StringUtils.isBlank(newTunnelURL)) {
            ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL
                .removeValue(getController());
        } else {
            ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL.setValue(
                getController(), newTunnelURL);
        }
        return true;
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
        // Why?
        // // Put on friendslist
        // if (!isTempServerNode(server)) {
        // if (!server.isFriend()) {
        // server.setFriend(true, null);
        // }
        // }
        // Re-initalize the service stubs on new server node.
        initializeServiceStubs();
    }

    private void initializeServiceStubs() {
        securityService = serviceProvider.getService(SecurityService.class);
        userService = serviceProvider.getService(AccountService.class);
        folderService = serviceProvider.getService(FolderService.class);
        publicKeyService = serviceProvider.getService(PublicKeyService.class);
    }

    private class RemoteServiceProvider implements ServiceProvider {
        public <T> T getService(Class<? extends T> serviceInterface) {
            return RemoteServiceStubFactory.createRemoteStub(getController(),
                serviceInterface, server);
        }
    }

    private void setAnonAccount() {
        accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
    }

    /**
     * TODO Support saving password with md5/salt
     */
    private void saveLastKnowLogin() {
        if (StringUtils.isBlank(username)) {
            getController().getPreferences().remove(
                PREFS_PREFIX + '.' + server.getIP() + ".username");
        } else {
            getController().getPreferences().put(
                PREFS_PREFIX + '.' + server.getIP() + ".username", username);
        }

        if (isRememberPassword() && !StringUtils.isBlank(password)) {
            getController().getPreferences().put(
                PREFS_PREFIX + '.' + server.getIP() + ".info2",
                Base64.encodeBytes(password.getBytes(Convert.UTF8)));
        } else {
            getController().getPreferences().remove(
                PREFS_PREFIX + '.' + server.getIP() + ".info2");
        }

        // Clear plain text password (TRAC #1291)
        getController().getPreferences().remove(
            PREFS_PREFIX + '.' + server.getIP() + ".info");
    }

    private void changeToServer(ServerInfo newServerInfo) {
        logFine("Changing server to " + newServerInfo.getNode());

        // Add key of new server to keystore.
        if (ProUtil.getPublicKey(getController(), newServerInfo.getNode()) == null)
        {
            PublicKey serverKey = publicKeyService.getPublicKey(newServerInfo
                .getNode());
            if (serverKey != null) {
                logFine("Retrieved new key for server "
                    + newServerInfo.getNode() + ". " + serverKey);
                ProUtil.addNodeToKeyStore(getController(), newServerInfo
                    .getNode(), serverKey);
            }
        }

        // Get new server node from local p2p nodemanager database
        Member newServerNode = newServerInfo.getNode().getNode(getController(),
            true);

        // Remind new server for next connect.
        if (updateConfig) {
            if (newServerInfo.getNode().getConnectAddress() != null) {
                setServerInConfig(newServerInfo.getNode());
            } else {
                // Fallback, use node INFO from P2P database.
                setServerInConfig(newServerNode.getInfo());
            }
            setServerWebURLInConfig(newServerInfo.getWebUrl());
            setServerHTTPTunnelURLInConfig(newServerInfo.getHTTPTunnelUrl());
            getController().saveConfig();
        }

        // Now actually switch to new server.
        setNewServerNode(newServerNode);
        login(username, password);
        // Attempt to login. At least remind login for real connect.
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

    /**
     * @return the string representing the server address
     */
    public String getServerString() {
        String addrStr;
        if (server != null) {
            if (server.isMySelf()) {
                addrStr = "myself";
            } else {
                InetSocketAddress addr = server.getReconnectAddress();
                addrStr = addr != null && addr.getAddress() != null
                    ? NetworkUtil.getHostAddressNoResolve(addr.getAddress())
                    : "n/a";
                if (addr != null
                    && addr.getPort() != ConnectionListener.DEFAULT_PORT)
                {
                    addrStr += ":" + addr.getPort();
                }
            }
        } else {
            addrStr = "n/a";
        }
        return addrStr;
    }

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
    private class MyNodeManagerListener extends NodeManagerAdapter {
        public void nodeConnected(NodeManagerEvent e) {
            // logWarning("Is server " + e.getNode() + "? " +
            // isServer(e.getNode()));
            if (isServer(e.getNode())) {
                // Our server member instance is a temporary one. Lets get real.
                if (isTempServerNode(server)) {
                    // Got connect to server! Take his ID and name.
                    Member oldServer = server;
                    setNewServerNode(e.getNode());
                    // Remove old temporary server entry without ID.
                    getController().getNodeManager().removeNode(oldServer);
                    if (updateConfig) {
                        setServerInConfig(server.getInfo());
                        getController().saveConfig();
                    }
                    logFine("Got connect to server: " + server);
                }

                listenerSupport.serverConnected(new ServerClientEvent(
                    ServerClient.this, e.getNode()));

                if (username != null) {
                    login(username, password);
                }
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (isServer(e.getNode())) {
                // Invalidate account.
                setAnonAccount();
                listenerSupport.serverDisconnected(new ServerClientEvent(
                    ServerClient.this, e.getNode()));
            }
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
            if (!getController().getNodeManager().isStarted()
                || !getController().getReconnectManager().isStarted())
            {
                return;
            }
            if (server.isConnecting() || server.isConnected()) {
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
