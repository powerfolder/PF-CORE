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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.sun.security.auth.callback.TextCallbackHandler;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.distribution.AbstractDistribution;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.AdminPermission;
import de.dal33t.powerfolder.security.AnonymousAccount;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import de.dal33t.powerfolder.security.NotLoggedInException;
import de.dal33t.powerfolder.security.SecurityException;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import edu.kit.scc.dei.ecplean.ECPAuthenticationException;
import edu.kit.scc.dei.ecplean.ECPAuthenticator;
import edu.kit.scc.dei.ecplean.ECPUnauthorizedException;

/**
 * Client to a server.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClient extends PFComponent {
    private static final String MEMBER_ID_TEMP_PREFIX = "TEMP_IDENTITY_";

    /**
     * If the current thread which processes Member.handleMessage is the server.
     */
    public static final ThreadLocal<Boolean> SERVER_HANDLE_MESSAGE_THREAD = new ThreadLocal<Boolean>()
    {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    // The last used username and password.
    // Tries to re-login with these if re-connection happens
    private String username;
    private String passwordObf;

    private String shibUsername;
    private String shibToken;

    private Member server;
    private final MyThrowableHandler throwableHandler = new MyThrowableHandler();
    private final AtomicBoolean loggingIn = new AtomicBoolean();
    private final AtomicBoolean loginExecuted = new AtomicBoolean(false);

    /**
     * ONLY FOR TESTS: If this client should connect to the server where it is
     * assigned to.
     */
    private boolean allowServerChange;

    // Prevent HAMMERING on the cluster.
    private int recentServerSwitches;
    private Date recentServerSwitch;
    /**
     * Update the config with new HOST/ID infos if retrieved from server.
     */
    private boolean updateConfig;

    /**
     * #2366: Quick login during handshake supported
     */
    private boolean supportsQuickLogin;

    /**
     * Log that is kept to synchronize calls to login
     */
    private final Object loginLock = new Object();

    /**
     * PFC-2534: remember last IdP and number of login retries to skip after
     * unauthorized login try
     */
    private String lastIdPUsed;
    private int shibbolethUnauthRetriesSkip;

    private AccountDetails accountDetails;

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
        super(controller);
        String name = ConfigurationEntry.SERVER_NAME.getValue(controller);
        String host = ConfigurationEntry.SERVER_HOST.getValue(controller);
        String nodeId = ConfigurationEntry.SERVER_NODEID.getValue(controller);

        if (!ConfigurationEntry.SERVER_NODEID.hasValue(controller)) {
            if (ConfigurationEntry.SERVER_HOST.hasValue(controller)) {
                // Hostname set, but no node id?
                nodeId = null;
            }
        }

        boolean allowServerChange = true;
        boolean updateConfig = ConfigurationEntry.SERVER_CONFIG_UPDATE
            .getValueBoolean(controller);

        init(controller, name, host, nodeId, allowServerChange, updateConfig);
    }

    public ServerClient(Controller controller, String name, String host,
        String nodeId, boolean allowServerChange, boolean updateConfig)
    {
        super(controller);
        init(controller, name, host, nodeId, allowServerChange, updateConfig);
    }

    /**
     * Constructs a server client with the defaults from the config.
     *
     * @param controller
     * @param name
     * @param host
     * @param nodeId
     * @param allowServerChange
     * @param updateConfig
     */
    private void init(Controller controller, String name, String host,
        String nodeId, boolean allowServerChange, boolean updateConfig)
    {
        this.allowServerChange = allowServerChange;
        this.updateConfig = updateConfig;
        supportsQuickLogin = true;

        // Custom server
        String theName = StringUtils.isBlank(name) ? Translation
            .getTranslation("online_storage.connecting") : name;

        boolean temporaryNode = StringUtils.isBlank(nodeId);
        String theNodeId = temporaryNode ? MEMBER_ID_TEMP_PREFIX + '|'
            + IdGenerator.makeId() : nodeId;
        Member theNode = controller.getNodeManager().getNode(theNodeId);
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
        if (StringUtils.isNotBlank(host)) {
            theNode.getInfo().setConnectAddress(
                Util.parseConnectionString(host));
        }

        if (theNode.getReconnectAddress() == null) {
            logSevere("Got server without reconnect address: " + theNode);
        }
        if (!ProUtil.isSwitchData(controller)) {
            logInfo("Using server: " + theNode.getNick() + ", ID: "
                + theNodeId + " @ " + theNode.getReconnectAddress());
        }
        init(theNode, allowServerChange);
    }

    private void init(Member serverNode, boolean serverChange) {
        Reject.ifNull(serverNode, "Server node is null");
        boolean firstCall=listenerSupport == null;
        if (firstCall) {
            listenerSupport = ListenerSupportFactory
                .createListenerSupport(ServerClientListener.class);
        }
        setNewServerNode(serverNode);
        // Allowed by default
        allowServerChange = serverChange;
        setAnonAccount();

        if (firstCall) {
            getController().getNodeManager().addNodeManagerListener(
                new MyNodeManagerListener());
            getController().getFolderRepository().addFolderRepositoryListener(
                new MyFolderRepositoryListener());
        }
    }

    private boolean isRememberPassword() {
        return PreferencesEntry.SERVER_REMEMBER_PASSWORD
            .getValueBoolean(getController());
    }

    // Basics *****************************************************************

    public void start() {
        boolean allowLAN2Internet = ConfigurationEntry.SERVER_CONNECT_FROM_LAN_TO_INTERNET
            .getValueBoolean(getController());
        if (!allowLAN2Internet && getController().isLanOnly()
            && !server.isOnLAN())
        {
            logWarning("Not connecting to server: " + server
                + ". Reason: Server not on LAN");
        }
        getController().scheduleAndRepeat(new ServerConnectTask(), 3L * 1000L,
            1000L * 20);
        getController().scheduleAndRepeat(new AutoLoginTask(), 10L * 1000L,
            1000L * 30);
        // Wait 10 seconds at start
        getController().scheduleAndRepeat(new HostingServersConnector(),
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
     * @param conHan
     * @return true if the node is the primary login server for the current
     *         account. account.
     */
    public boolean isPrimaryServer(ConnectionHandler conHan) {
        if (server.getInfo().equals(conHan.getIdentity().getMemberInfo())) {
            return true;
        }
        if (isTempServerNode(server)) {
            if (server.getReconnectAddress().equals(conHan.getRemoteAddress()))
            {
                return true;
            }
            // Try check by hostname / port
            InetSocketAddress nodeSockAddr = conHan.getRemoteAddress();
            InetSocketAddress serverSockAddr = server.getReconnectAddress();
            if (nodeSockAddr == null || serverSockAddr == null) {
                return false;
            }
            InetAddress nodeAddr = nodeSockAddr.getAddress();
            InetAddress serverAddr = serverSockAddr.getAddress();
            if (nodeAddr == null || serverAddr == null) {
                return false;
            }
            String nodeHost = NetworkUtil.getHostAddressNoResolve(nodeAddr);
            String serverHost = NetworkUtil.getHostAddressNoResolve(serverAddr);
            int nodePort = nodeSockAddr.getPort();
            int serverPort = serverSockAddr.getPort();
            return nodeHost.equalsIgnoreCase(serverHost)
                && nodePort == serverPort;
        }
        return false;
    }

    /**
     * @param node
     * @return true if the node is the primary login server for the current
     *         account. account.
     */
    public boolean isPrimaryServer(Member node) {
        if (server.equals(node)) {
            return true;
        }
        if (isTempServerNode(server)) {
            if (server.getReconnectAddress().equals(node.getReconnectAddress()))
            {
                return true;
            }
            // Try check by hostname / port
            InetSocketAddress nodeSockAddr = node.getReconnectAddress();
            InetSocketAddress serverSockAddr = server.getReconnectAddress();
            if (nodeSockAddr == null || serverSockAddr == null) {
                return false;
            }
            InetAddress nodeAddr = nodeSockAddr.getAddress();
            InetAddress serverAddr = serverSockAddr.getAddress();
            if (nodeAddr == null || serverAddr == null) {
                return false;
            }
            String nodeHost = NetworkUtil.getHostAddressNoResolve(nodeAddr);
            String serverHost = NetworkUtil.getHostAddressNoResolve(serverAddr);
            int nodePort = nodeSockAddr.getPort();
            int serverPort = serverSockAddr.getPort();
            return nodeHost.equalsIgnoreCase(serverHost)
                && nodePort == serverPort;
        }
        return false;
    }

    /**
     * @param node
     * @return true if the node is a part of the server cloud.
     */
    public boolean isClusterServer(Member node) {
        return node.isServer() || isPrimaryServer(node);
    }

    /**
     * @return all KNOWN servers of the cluster
     */
    public Collection<Member> getServersInCluster() {
        List<Member> servers = new LinkedList<Member>();
        for (Member node : getController().getNodeManager()
            .getNodesAsCollection())
        {
            if (node.isServer()) {
                servers.add(node);
            }
        }
        // Every day I'm shuffleing
        Collections.shuffle(servers);
        return servers;
    }

    /**
     * Sets/Changes the server.
     *
     * @param serverNode
     * @param allowServerChange
     */
    public void setServer(Member serverNode, boolean allowServerChange) {
        Reject.ifNull(serverNode, "Server node is null");
        setNewServerNode(serverNode);
        this.allowServerChange = allowServerChange;
        if (StringUtils.isBlank(username) || StringUtils.isBlank(passwordObf)) {
            loginWithLastKnown();
        } else {
            login(username, passwordObf, true);
        }
        if (!isConnected()) {
            server.markForImmediateConnect();
        }
    }

    /**
     * @return if the server is connected
     */
    public boolean isConnected() {
        return server.isMySelf() || server.isConnected();
    }

    /**
     * @return the URL of the web access to the server (cluster).
     */
    public String getWebURL() {
        String webURL = Util
            .removeLastSlashFromURI(ConfigurationEntry.SERVER_WEB_URL
                .getValue(getController()));
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

    public String getWebURL(String uri, boolean withCredentials) {
        if (!hasWebURL()) {
            return null;
        }
        String webURL = getWebURL();
        if (StringUtils.isBlank(uri)) {
            uri = "";
        }
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        if (!withCredentials
            || !ConfigurationEntry.WEB_PASSWORD_ALLOWED
                .getValueBoolean(getController()))
        {
            return webURL + '/' + uri;
        }
        String fullURL = getLoginURLWithCredentials();
        try {
            if (fullURL.contains("?")) {
                fullURL += "&";
            } else {
                fullURL += "?";
            }
            fullURL += Constants.LOGIN_PARAM_ORIGINAL_URI + "="
                + URLEncoder.encode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return fullURL;
    }

    /**
     * @return true if the connected server offers a web interface.
     */
    public boolean hasWebURL() {
        return getWebURL() != null;
    }

    /**
     * #2488
     *
     * @return true if web DAV is available at the server.
     */
    public boolean supportsWebDAV() {
        if (!hasWebURL()) {
            return false;
        }
        return ConfigurationEntry.WEB_DAV_ENABLED
            .getValueBoolean(getController());
    }

    /**
     * #2488
     *
     * @return true if web login as regular user is allowed at the server.
     */
    public boolean supportsWebLogin() {
        if (!hasWebURL()) {
            return false;
        }
        if (ConfigurationEntry.WEB_LOGIN_ALLOWED
            .getValueBoolean(getController()))
        {
            return true;
        }
        if (accountDetails == null) {
            return false;
        }
        return accountDetails.getAccount().hasPermission(
            AdminPermission.INSTANCE);
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

        // PFS-862: Start
        if (isLoggedIn()) {
            try {
                String otp = getSecurityService().requestOTP();
                if (isFine()) {
                    logFine("Retrieved OTP for "
                        + accountDetails.getAccount().getUsername() + ": "
                        + otp);
                }
                if (LoginUtil.isOTPValid(otp)) {
                    return LoginUtil.decorateURL(
                        getWebURL(Constants.LOGIN_URI, false), null, otp);
                }
            } catch (Exception e) {
                // Not supported. Maybe old server version. Ignore
                logFine("Unable to generate OTP. " + e);
            }
        }
        // PFS-862: End

        if (!ConfigurationEntry.WEB_PASSWORD_ALLOWED
            .getValueBoolean(getController()))
        {
            return getWebURL();
        }
        return LoginUtil.decorateURL(getWebURL(Constants.LOGIN_URI, false),
            username, passwordObf);
    }

    /**
     * @param foInfo
     * @return the direct URL to the folder
     */
    public String getFolderURL(FolderInfo foInfo) {
        if (!hasWebURL()) {
            return null;
        }
        return getWebURL("/files/" + Base64.encode4URL(foInfo.id), false);
    }

    /**
     * @param foInfo
     * @return the direct URL to the folder including login if necessary
     */
    public String getFolderURLWithCredentials(FolderInfo foInfo) {
        if (!supportsWebLogin()) {
            return null;
        }
        String folderURI = getFolderURL(foInfo);
        folderURI = folderURI.replace(getWebURL(), "");
        String loginURL = getLoginURLWithCredentials();
        if (loginURL.contains("?")) {
            loginURL += "&";
        } else {
            loginURL += "?";
        }
        loginURL += Constants.LOGIN_PARAM_ORIGINAL_URI;
        loginURL += "=";
        loginURL += folderURI;
        return loginURL;
    }

    /**
     * #2675: Shell integration.
     *
     * @param fInfo
     * @return
     */
    public String getFileLinkURL(FileInfo fInfo) {
        Reject.ifNull(fInfo, "fileinfo");
        if (!hasWebURL()) {
            return null;
        }
        return getWebURL(
            Constants.GET_LINK_URI + '/'
                + Base64.encode4URL(fInfo.getFolderInfo().getId()) + '/'
                + Util.endcodeForURL(fInfo.getRelativeName()), true);
    }

    /**
     * @return if password recovery is supported
     */
    public boolean supportsRecoverPassword() {
        return StringUtils.isNotBlank(getRecoverPasswordURL());
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
        if (!ConfigurationEntry.SERVER_RECOVER_PASSWORD_ENABLED
            .getValueBoolean(getController()))
        {
            return null;
        }
        String url = getWebURL(Constants.LOGIN_URI, false);
        if (StringUtils.isNotBlank(username)) {
            url = LoginUtil.decorateURL(url, username, (char[]) null);
        }
        return url;
    }

    /**
     * @return true if client supports register on registerURL.
     */
    public boolean supportsWebRegistration() {
        return ConfigurationEntry.SERVER_REGISTER_ENABLED
            .getValueBoolean(getController());
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
        return getWebURL(Constants.REGISTER_URI, false);
    }

    /**
     * Convenience method for getting register URL
     * 
     * @return the registration URL for this server.
     */
    public String getRegisterURLReferral() {
        String url = getRegisterURL();
        if (StringUtils.isBlank(url)) {
            return getWebURL();
        }
        if (!isLoggedIn()) {
            return url;
        }
        try {
            if (url.contains("?")) {
                url += "&";
            } else {
                url += "?";
            }
            return url + "ref="
                + URLEncoder.encode(getAccount().getOID(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
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
        return getWebURL(Constants.ACTIVATE_URI, false);
    }

    /**
     * Convenience method to get the URL to an avatar
     *
     * @param information about the account
     * @return the avatar URL.
     */
    public String getAvatarURL(AccountInfo aInfo, boolean thumbnail) {
        if (!hasWebURL()) {
            return null;
        }
        StringBuilder url = new StringBuilder();
        url.append("/avatars/user/");
        url.append(aInfo.getOID());
        if (thumbnail) {
            url.append("_tn");
        }
        return getWebURL(url.toString(), false);
    }

    /**
     * @return if all new folders should be backed up by the server/cloud.
     */
    public boolean isBackupByDefault() {
        return PreferencesEntry.USE_ONLINE_STORAGE
            .getValueBoolean(getController())
            || getController().isBackupOnly()
            || !PreferencesEntry.EXPERT_MODE.getValueBoolean(getController());
    }

    // Login ******************************************************************

    /**
     * @return true if we know last login data. uses default account setting as
     *         fallback
     */
    public boolean isLastLoginKnown() {
        return ConfigurationEntry.SERVER_CONNECT_USERNAME
            .hasValue(getController());
    }

    /**
     * Tries to logs in with the last know username/password combination for
     * this server.uses default account setting as fallback
     *
     * @return the identity with this username or <code>InvalidAccount</code> if
     *         login failed.
     */
    public Account loginWithLastKnown() {

        String un = null;
        char[] pw = null;

        if (ConfigurationEntry.SERVER_CONNECT_USERNAME
            .hasValue(getController()))
        {
            un = ConfigurationEntry.SERVER_CONNECT_USERNAME
                .getValue(getController());
            pw = LoginUtil
                .deobfuscate(ConfigurationEntry.SERVER_CONNECT_PASSWORD
                    .getValue(getController()));

            if (pw == null) {
                String pws = ConfigurationEntry.SERVER_CONNECT_PASSWORD_CLEAR
                    .getValue(getController());
                if (StringUtils.isNotBlank(pws)) {
                    pw = Util.toCharArray(pws);
                }
            }
        }

        if (StringUtils.isNotBlank(getController().getCLIUsername())) {
            un = getController().getCLIUsername();
        }
        if (StringUtils.isNotBlank(getController().getCLIPassword())) {
            pw = Util.toCharArray(getController().getCLIPassword());
        }

        if (ConfigurationEntry.SERVER_CONNECT_NO_PASSWORD_ALLOWED
            .getValueBoolean(getController()))
        {
            if (StringUtils.isBlank(un)) {
                un = System.getProperty("user.name");
            }
            if (pw == null || pw.length == 0) {
                pw = Util.toCharArray(ProUtil.rtrvePwssd(getController(), un));
            }
        }

        String systemUserName = System.getProperty("user.name");
        // PFC-2533: Don't use it for CFN
        // if (StringUtils.isBlank(un)
        // && LoginUtil.isValidUsername(getController(), systemUserName))
        // {
        // un = systemUserName;
        // }

        if (StringUtils.isBlank(un)
            && (pw == null || pw.length == 0)
            && ConfigurationEntry.KERBEROS_SSO_ENABLED
                .getValueBoolean(getController())) {
            un = systemUserName;
        }

        if (StringUtils.isBlank(un)) {
            logFine("Not logging in. Username blank");
        } else {
            if (!ProUtil.isSwitchData(getController()))
            {
                logInfo("Logging into server " + getServerString()
                    + ". Username: " + un);
            }
            return login(un, pw);
        }
        // Failed!
        return null;
    }

    /**
     * Log out of online storage.
     */
    public void logout() {
        username = null;
        passwordObf = null;
        try {
            securityService.logout();
        } catch (Exception e) {
            logWarning("Unable to logout. " + e);
        }
        saveLastKnowLogin(null, null);
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
    public Account login(String theUsername, char[] thePassword) {
        return login(theUsername, LoginUtil.obfuscate(thePassword), true);
    }

    /**
     * Logs into the server and saves the identity as my login.
     * <p>
     * If the server is not connected and invalid account is returned and the
     * login data saved for auto-login on reconnect.
     *
     * @param theUsername
     * @param thePasswordObj
     *            the obfuscated password
     * @param saveLastLogin
     *            if the last login should be remembered.
     * @return the identity with this username or <code>InvalidAccount</code> if
     *         login failed. NEVER returns <code>null</code>
     */
    public Account login(String theUsername, String thePasswordObj,
        boolean saveLastLogin)
    {
        logFine("Login with: " + theUsername);
        synchronized (loginLock) {
            loggingIn.set(true);
            String prevUsername = username;
            String prevPasswordObf = passwordObf;
            try {
                username = theUsername;
                passwordObf = thePasswordObj;
                if (saveLastLogin) {
                    saveLastKnowLogin(username, passwordObf);
                }
                boolean disconnected = !server.isConnected();
                boolean pwEmpty = StringUtils.isBlank(passwordObf);
                boolean noKerberosLogin = !isKerberosLogin();
                if (disconnected || (pwEmpty && noKerberosLogin)) {
                    setAnonAccount();
                    fireLogin(accountDetails);
                    return accountDetails.getAccount();
                }
                boolean loginOk = false;
                char[] pw = LoginUtil.deobfuscate(passwordObf);
                try {
                    if (isShibbolethLogin()) {
                        // PFC-2534: Start
                        try {
                            String currentIdP = ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP
                                .getValue(getController());
                            boolean idpEqual = StringUtils.isEqual(lastIdPUsed,
                                currentIdP);
                            boolean pwEqual = StringUtils.isEqual(
                                prevPasswordObf, passwordObf);
                            boolean unEqual = StringUtils.isEqual(prevUsername,
                                username);
                            if (shibbolethUnauthRetriesSkip != 0 && unEqual
                                && pwEqual && idpEqual)
                            {
                                shibbolethUnauthRetriesSkip--;
                                if (isFine()) {
                                    logFine("Skipping login another "
                                        + shibbolethUnauthRetriesSkip
                                        + " times");
                                }
                                setAnonAccount();
                                return accountDetails.getAccount();
                            }

                            lastIdPUsed = currentIdP;
                            shibbolethUnauthRetriesSkip = 0;
                        } catch (RuntimeException e) {
                            logWarning("An error occured skipping shibboleth login: "
                                + e);
                        }
                        // PFC-2534: End

                        boolean externalUser = prepareShibbolethLogin(
                            username,
                            pw,
                            (prevUsername != null && !prevUsername
                                .equals(username))
                                || (prevPasswordObf != null && !prevPasswordObf
                                    .equals(passwordObf)));
                        if (externalUser) {
                            loginOk = securityService.login(username, pw);
                        } else if (shibUsername != null && shibToken != null) {
                            loginOk = securityService.login(shibUsername,
                                Util.toCharArray(shibToken));
                        } else {
                            logWarning("Neither Shibboleth nor external login possible!");
                        }
                    } else if (isKerberosLogin()) {
                        byte[] serviceTicket = prepareKerberosLogin();
                        loginOk = securityService
                            .login(username, serviceTicket);
                    } else {
                        loginOk = securityService.login(username, pw);
                    }

                    loginExecuted.set(true);
                } catch (RemoteCallException e) {
                    if (e.getCause() instanceof NoSuchMethodException) {
                        // Old server version (Pre 1.5.0 or older)
                        // Try it the old fashioned way
                        logSevere("Client incompatible with server: Server version too old");
                    }
                    // Rethrow
                    throw e;
                } finally {
                    LoginUtil.clear(pw);
                }

                if (!loginOk) {
                    logWarning("Login to server " + server + " (user "
                        + theUsername + ") failed!");
                    setAnonAccount();
                    fireLogin(accountDetails, false);
                    return accountDetails.getAccount();
                }
                AccountDetails newAccountDetails = securityService
                    .getAccountDetails();
                logInfo("Login to server " + server.getReconnectAddress()
                    + " (user " + theUsername + ") result: "
                    + newAccountDetails);
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
                    loggingIn.set(false);
                    fireLogin(accountDetails);
                    getController().schedule(new Runnable() {
                        @Override
                        public void run() {
                            // Also switches server
                            updateLocalSettings(accountDetails.getAccount());
                        }
                    }, 0);
                } else {
                    setAnonAccount();
                    fireLogin(accountDetails, false);
                }
                return accountDetails.getAccount();
            } catch (Exception e) {
                logWarning("Unable to login: " + e);
                if (isShibbolethLogin()) {
                    // PFC-2534: Start
//                    username = prevUsername;
//                    passwordObf = prevPasswordObf;
                    // PFC-2534: End
                    saveLastKnowLogin(username, passwordObf);
                }
                setAnonAccount();
                fireLogin(accountDetails, false);
                return accountDetails.getAccount();
            } finally {
                loggingIn.set(false);
            }
        }
    }

    private boolean isKerberosLogin() {
        return ConfigurationEntry.KERBEROS_SSO_ENABLED
            .getValueBoolean(getController())
            && StringUtils.isBlank(passwordObf);
    }

    private byte[] prepareKerberosLogin() {
        try {
            Path outputFile = Controller.getTempFilesLocation().resolve(
                "login.conf");

            if (Files.notExists(outputFile)) {
                InputStream configFile = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("kerberos/login.conf");
                PathUtils.copyFromStreamToFile(configFile, outputFile);
            }

            System.setProperty("java.security.auth.login.config", outputFile
                .toAbsolutePath().toString());

            System
                .setProperty("java.security.krb5.realm",
                    ConfigurationEntry.KERBEROS_SSO_REALM
                        .getValue(getController()));
            String kdc = ConfigurationEntry.KERBEROS_SSO_KDC
                .getValue(getController());
            System.setProperty("java.security.krb5.kdc", kdc);

            LoginContext lc = new LoginContext("SignedOnUserLoginContext",
                new TextCallbackHandler());
            lc.login();
            Subject clientSubject = lc.getSubject();

            username = clientSubject.getPrincipals().iterator().next()
                .getName();
            return Subject.doAs(clientSubject, new ServiceTicketGenerator());
        } catch (Exception e) {
            logWarning("Unable to login: " + e);
            return null;
        } finally {
            loggingIn.set(false);
        }
    }

    private boolean isShibbolethLogin() {
        return ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
            .hasValue(getController());
    }

    public boolean isAllowedToCreateFolders() {
        if (getAccount().getOSSubscription().getStorageSize() <= 0) {
            return false;
        }
        if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
            .getValueBoolean(getController())
            && !getAccount().hasPermission(FolderCreatePermission.INSTANCE))
        {
            return false;
        }
        return true;
    }

    /**
     * Prepare login information for shibboleth environment.
     *
     * @param username
     * @param thePassword
     * @param userChanged
     * @return True if the user should login as external user, false if
     *         shibboleth is used.
     */
    private boolean prepareShibbolethLogin(String username, char[] thePassword, boolean userChanged) {
        String idpURLString = ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP
            .getValue(getController());

        if (StringUtils.isBlank(idpURLString)) {
            shibUsername = null;
            shibToken = null;
            throw new SecurityException("Your organization is unreachable");
        } else if (userChanged) {
            shibUsername = null;
            shibToken = null;
        } else if ("ext".equals(idpURLString)) {
            return true;
        }

        boolean tokenIsValid = false;
        try {
            tokenIsValid = shibToken != null
                && shibToken.contains(":")
                && System.currentTimeMillis() <= Long.valueOf(shibToken
                    .substring(shibToken.indexOf(':') + 1, shibToken.length()));
        } catch (Exception e) {
            logFine("Unusable Shibboleth Token: " + shibToken + " valid ? "
                + tokenIsValid);
            shibUsername = null;
            shibToken = null;
        }
        if (StringUtils.isBlank(shibUsername) || StringUtils.isBlank(shibToken)
            || !tokenIsValid)
        {
            String spURL = getWebURL(Constants.LOGIN_SHIBBOLETH_CLIENT_URI
                + '/' + getController().getMySelf().getId(), false);
            URI spURI;
            try {
                spURI = new URI(spURL);
            } catch (URISyntaxException e) {
                shibUsername = null;
                shibToken = null;
                // Should not happen
                throw new RuntimeException(
                    "Unable to resolve service provider URL: " + spURL + ". "
                        + e);
            }

            URI idpURI = null;
            try {
                idpURI = new URI(idpURLString);
            } catch (Exception e) {
                shibUsername = null;
                shibToken = null;
                // Should not happen
                throw new RuntimeException(
                    "Unable to resolve identity provider URL: "
                        + ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP
                            .getValue(getController()) + ". " + e);
            }

            // PFC-2496: Start
            DefaultHttpClient dhc = new DefaultHttpClient();

            // Set Proxy credentials, if configured
            if (ConfigurationEntry.HTTP_PROXY_HOST.hasValue(getController())) {
                String proxyUsername = "";
                String proxyPassword = "";

                if (ConfigurationEntry.HTTP_PROXY_USERNAME.hasValue(getController())) {
                    proxyUsername = ConfigurationEntry.HTTP_PROXY_USERNAME.getValue(getController());

                    if (ConfigurationEntry.HTTP_PROXY_PASSWORD.hasValue(getController())) {
                        proxyPassword = ConfigurationEntry.HTTP_PROXY_PASSWORD.getValue(getController());
                    }
                }

                dhc.getCredentialsProvider().setCredentials(
                    new AuthScope(ConfigurationEntry.HTTP_PROXY_HOST
                        .getValue(getController()),
                        ConfigurationEntry.HTTP_PROXY_PORT
                            .getValueInt(getController())),
                    new UsernamePasswordCredentials(
                        proxyUsername,
                        proxyPassword));

                HttpHost proxy = new HttpHost(
                    ConfigurationEntry.HTTP_PROXY_HOST
                        .getValue(getController()),
                    ConfigurationEntry.HTTP_PROXY_PORT
                        .getValueInt(getController()));
                dhc.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
            // PFC-2496: End

            ECPAuthenticator auth = new ECPAuthenticator(dhc, username, new String(
                thePassword), idpURI, spURI);
            String[] result;
            try {
                result = auth.authenticate();
                shibUsername = result[0];
                shibToken = result[1];
            } catch (ECPUnauthorizedException e) {
                shibbolethUnauthRetriesSkip = ConfigurationEntry.SERVER_LOGIN_SKIP_RETRY
                    .getValueInt(getController());
                shibUsername = null;
                shibToken = null;
                throw new SecurityException(e);
            } catch (ECPAuthenticationException e) {
                shibUsername = null;
                shibToken = null;
                throw new SecurityException(e);
            }
        }

        return false;
    }

    private void findAlternativeServer() {
        if (!allowServerChange) {
            return;
        }
        if (getController().getMySelf().isServer()) {
            // Don't
            return;
        }
        if (getController().isShuttingDown() || !getController().isStarted()) {
            return;
        }
        if (isFine()) {
            logFine("findAlternativeServer: " + getServersInCluster());
        }
        for (Member server : getServersInCluster()) {
            if (!server.isConnected()) {
                server.markForImmediateConnect();
            }
            Waiter w = new Waiter(500);
            while (w.isTimeout() && !server.isConnected()) {
                w.waitABit();
            }
            if (server.isConnected()) {
                if (!server.equals(this.server)) {
                    logInfo("Switching to new server: " + server);
                    try {
                        setServer(server, allowServerChange);
                        break;
                    } catch (Exception e) {
                        logWarning("Unable to switch server to "
                            + server.getNick() + ". Searching for new..." + e);
                    }
                }
            }
        }
    }

    /**
     * Load a new configuration from URL configURL
     *
     * @param configURL
     */
    public void loadConfigURL(String configURL) {
        Reject.ifBlank(configURL, "configURL");
        try {
            // load the configuration from the url ...
            Properties props = ConfigurationLoader
                .loadPreConfiguration(configURL.trim());

            ConfigurationLoader.merge(props, getController());
            String networkID = (String) props.get(ConfigurationEntry.NETWORK_ID
                .getConfigKey());
            String name = (String) props.get(ConfigurationEntry.SERVER_NAME
                .getConfigKey());
            String host = (String) props.get(ConfigurationEntry.SERVER_HOST
                .getConfigKey());
            String nodeId = (String) props.get(ConfigurationEntry.SERVER_NODEID
                .getConfigKey());
            String tunnelURL = (String) props
                .get(ConfigurationEntry.SERVER_HTTP_TUNNEL_RPC_URL
                    .getConfigKey());
            String webURL = (String) props
                .get(ConfigurationEntry.SERVER_WEB_URL.getConfigKey());

            logInfo("Loaded " + props.size() + " from " + configURL + " network ID: " + networkID);
            if (StringUtils.isBlank(host)) {
                throw new IOException("Hostname not found");
            }

            String oldNetworkID = getController().getMySelf().getInfo().networkId;
            if (StringUtils.isNotBlank(networkID)) {
                getController().getMySelf().getInfo().networkId = networkID;
            } else {
                getController().getMySelf().getInfo().networkId = ConfigurationEntry.NETWORK_ID
                    .getDefaultValue();
            }
            String newNetworkID = getController().getMySelf().getInfo().networkId;
            boolean networkIDChanged = !Util.equals(oldNetworkID, newNetworkID);
            if (networkIDChanged) {
                getController().getNodeManager().shutdown();
            }


            init(getController(), name, host, nodeId, allowServerChange,
                updateConfig);

            // Store in config
            setServerWebURLInConfig(webURL);
            setServerHTTPTunnelURLInConfig(tunnelURL);
            setServerInConfig(getServer().getInfo());
            ConfigurationEntry.NETWORK_ID.setValue(getController(), newNetworkID);
            ConfigurationEntry.CONFIG_URL.setValue(getController(), configURL);

            getController().saveConfig();

            if (networkIDChanged) {
                // Restart nodemanager
                getController().getNodeManager().start();
            }

            connectHostingServers();
        } catch (Exception e) {
            logWarning("Could not load connection infos from " + configURL
                + ": " + e.getMessage());
        }
    }

    /**
     * Are we currently logging in?
     *
     * @return
     */
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    /**
     * @return true if the last attempt to login to the online storage was ok.
     *         false if not or no login tried yet.
     */
    public boolean isLoggedIn() {
        return getAccount() != null && getAccount().isValid();
    }

    /**
     * @return true if once a login call to the server was successfully executed.
     */
    public boolean isLoginExecuted() {
        return loginExecuted.get();
    }

    /**
     * @return the username that is set for login.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return true if the currently set password is empty.
     */
    public boolean isPasswordEmpty() {
        return StringUtils.isBlank(passwordObf);
    }

    /**
     *
     * @return true if the password is empty and Single Sign-on via Kerberos is disabled.
     */
    public boolean isPasswordRequired() {
        return StringUtils.isBlank(passwordObf)
            && !ConfigurationEntry.KERBEROS_SSO_ENABLED
                .getValueBoolean(getController());
    }

    /**
     * ATTENTION: Make sure the returned char array is purged/cleared as soon as
     * possible with {@link LoginUtil#clear(char[])}
     *
     * @return the password that is set for login.
     */
    public char[] getPassword() {
        return LoginUtil.deobfuscate(passwordObf);
    }

    /**
     * ATTENTION: This password must not be used for long. It cannot be
     * purged/cleared from memory.
     *
     * @return the password used in CLEAR TEXT.
     */
    public String getPasswordClearText() {
        char[] pw = LoginUtil.deobfuscate(passwordObf);
        String txt = Util.toString(pw);
        LoginUtil.clear(pw);
        return txt;
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
            updateLocalSettings(accountDetails.getAccount());
        } else {
            setAnonAccount();
            fireLogin(accountDetails, false);
        }
        if (isFine()) {
            logFine("Refreshed " + accountDetails);
        }
        return accountDetails;
    }

    private void updateLocalSettings(Account a) {
        updateServer(a);
        updateFriendsList(a);
        getController().getFolderRepository().updateFolders(a);
        scheduleConnectHostingServers();
    }

    private void updateServer(Account a) {
        // Possible switch to new server
        final ServerInfo targetServer = a.getServer();
        if (targetServer == null || !allowServerChange) {
            return;
        }
        // Not hosted on the server we just have logged into.
        boolean changeServer = !server.getInfo().equals(targetServer.getNode());
        if (!changeServer) {
            return;
        }
        final Member targetServerNode = targetServer.getNode().getNode(
            getController(), true);
        boolean checked = currentlyHammeringServers()
            || !targetServerNode.isConnected();
        if (!checked) {
            logInfo("Switching from " + server.getNick() + " to "
                + targetServerNode.getNick());
            changeToServer(targetServer);
        } else {
            if (currentlyHammeringServers()) {
                logInfo("Switching from " + server.getNick() + " to "
                    + targetServerNode.getNick() + " in " + HAMMER_DELAY / 1000
                    + "s");
            } else {
                logInfo("Switching from " + server.getNick() + " to "
                    + targetServerNode.getNick() + " after connect");
            }

            Waiter w = new Waiter(HAMMER_DELAY);
            while (!w.isTimeout()) {
                w.waitABit();
                if (!currentlyHammeringServers()
                    && targetServerNode.isConnected())
                {
                    break;
                }
            }
        }

        if (checked) {
            getController().getIOProvider().startIO(new Runnable() {
                @Override
                public void run() {
                    if (!targetServerNode.isConnected()) {
                        if (!isConnected()) {
                            logWarning("Unable to connect to server: "
                                + targetServerNode.getNick()
                                + ". Searching for alternatives...");
                            findAlternativeServer();
                        }
                    } else {
                        boolean changeServer = !server.getInfo().equals(
                            targetServer.getNode());
                        if (changeServer) {
                            changeToServer(targetServer);
                        }
                    }
                }
            });
        }
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

    public <T> T getService(Class<T> serviceInterface) {
        return RemoteServiceStubFactory.createRemoteStub(getController(),
            serviceInterface, server, throwableHandler);
    }

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
    public List<Folder> getJoinedCloudFolders() {
        List<Folder> mirroredFolders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            if (joinedByCloud(folder)) {
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
     * @return true if the cloud has joined the folder.
     */
    public boolean joinedByCloud(Folder folder) {
        if (folder.hasMember(server)) {
            return true;
        }
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isServer() && !member.isMySelf()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param foInfo
     *            the folder to check.
     * @return true if the cloud has joined the folder.
     */
    public boolean joinedByCloud(FolderInfo foInfo) {
        Folder folder = foInfo.getFolder(getController());
        if (folder != null) {
            return joinedByCloud(folder);
        }
        boolean folderInCloud = false;
        FolderList fList = server.getLastFolderList();
        ConnectionHandler conHan = server.getPeer();
        if (conHan != null && fList != null) {
            folderInCloud = fList.contains(foInfo, conHan.getMyMagicId());
        }
        // TODO: #2435
        return folderInCloud;
    }

    private void scheduleConnectHostingServers() {
        boolean currentlyHammering = currentlyHammeringServers();
        if (currentlyHammering) {
            logWarning("Detected hammering of server/cluster. Throttling reconnect speed. Next try in "
                + HAMMER_DELAY / 1000 + "s");
        }
        getController().schedule(new HostingServersConnector(),
            currentlyHammering ? HAMMER_DELAY : 1000L);
    }

    /**
     * Tries to connect hosting servers of our locally joined folders. Call this
     * when it is expected, that any of the locally joined folders is hosted on
     * another server. This method does NOT block, it instead schedules a
     * background task to retrieve and connect those servers.
     */
    private void connectHostingServers() {
        if (!(isConnected() || isLoggingIn() || isLoggedIn())) {
            findAlternativeServer();
            return;
        }
        if (isFiner()) {
            logFiner("Connecting to cluster servers");
        }
        Runnable retriever = new Runnable() {
            @Override
            public void run() {
                retrieveAndConnectoClusterServers();
            }
        };
        getController().getIOProvider().startIO(retriever);
    }

    private void retrieveAndConnectoClusterServers() {
        try {
            if (ConfigurationEntry.SERVER_LOAD_NODES
                .getValueBoolean(getController()))
            {
                getController().getNodeManager().loadServerNodes(this);
            }

            if (!isConnected() || !isLoggedIn()) {
                return;
            }

            Collection<FolderInfo> infos = getController()
                .getFolderRepository().getJoinedFolderInfos();
            FolderInfo[] folders = infos.toArray(new FolderInfo[infos.size()]);
            Collection<MemberInfo> hostingServers = getFolderService()
                .getHostingServers(folders);
            if (isFine()) {
                logFine("Got " + hostingServers.size() + " servers for our "
                    + folders.length + " folders: " + hostingServers);
            }
            for (MemberInfo hostingServerInfo : hostingServers) {
                Member hostingServer = hostingServerInfo.getNode(
                    getController(), true);
                hostingServer.updateInfo(hostingServerInfo);
                hostingServer.setServer(true);

                if (hostingServer.isConnected() || hostingServer.isConnecting()
                    || hostingServer.equals(server))
                {
                    // Already connected / reconnecting
                    continue;
                }
                // Connect now
                hostingServer.markForImmediateConnect();
            }
        } catch (Exception e) {
            logWarning("Unable to retrieve servers of cluster." + e);
        }
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
        if (isTempServerNode(newServer)) {
            ConfigurationEntry.SERVER_NODEID.removeValue(getController());
        } else {
            ConfigurationEntry.SERVER_NODEID.setValue(getController(),
                newServer.id);
        }
    }

    public boolean setServerWebURLInConfig(String newWebUrl) {
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
        logFine("New tunnel URL: " + newTunnelURL);
        String oldUrl = ConfigurationEntry.SERVER_HTTP_TUNNEL_RPC_URL
            .getValue(getController());
        if (Util.equals(oldUrl, newTunnelURL)) {
            return false;
        }
        // #2158: Don't override if we are a sever itself.
        if (getController().getMySelf().isServer()) {
            return false;
        }
        // Currently not supported from config
        if (StringUtils.isBlank(newTunnelURL)) {
            ConfigurationEntry.SERVER_HTTP_TUNNEL_RPC_URL
                .removeValue(getController());
        } else {
            ConfigurationEntry.SERVER_HTTP_TUNNEL_RPC_URL.setValue(
                getController(), newTunnelURL);
        }
        return true;
    }

    // Event handling ********************************************************

    public void addListener(ServerClientListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void addWeakListener(ServerClientListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener, true);
    }

    public void removeListener(ServerClientListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    // Internal ***************************************************************

    private void setNewServerNode(Member newServerNode) {
        // Hammering detection
        if (server != null && newServerNode != null) {
            boolean changed = !server.equals(newServerNode);
            if (changed) {
                if (recentServerSwitch != null
                    && (System.currentTimeMillis() - recentServerSwitch
                        .getTime()) > HAMMER_TIME)
                {
                    // Reset counter if last switch was "long" ago.
                    recentServerSwitches = 0;
                }
                recentServerSwitch = new Date();
                recentServerSwitches++;
            }
        }
        server = newServerNode;
        server.setServer(true);
        logInfo("New primary server: " + server.getNick());

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

    private static final long HAMMER_TIME = 10000L;
    private static final int HAMMER_HITS = 20;
    private static final long HAMMER_DELAY = 30000L;

    private boolean currentlyHammeringServers() {
        return recentServerSwitch != null
            && (System.currentTimeMillis() - recentServerSwitch.getTime()) <= HAMMER_TIME
            && recentServerSwitches >= HAMMER_HITS;
    }

    private void initializeServiceStubs() {
        securityService = getService(SecurityService.class);
        userService = getService(AccountService.class);
        folderService = getService(FolderService.class);
        publicKeyService = getService(PublicKeyService.class);
    }

    private void setAnonAccount() {
        accountDetails = new AccountDetails(new AnonymousAccount(), 0, 0);
    }

    private void saveLastKnowLogin(String username, String passwordObf) {
        if (StringUtils.isNotBlank(username)) {
            ConfigurationEntry.SERVER_CONNECT_USERNAME.setValue(
                getController(), username);
        } else {
            ConfigurationEntry.SERVER_CONNECT_USERNAME
                .removeValue(getController());
        }

        if (isRememberPassword() && StringUtils.isNotBlank(passwordObf)) {
            ConfigurationEntry.SERVER_CONNECT_PASSWORD.setValue(
                getController(), passwordObf);
        } else {
            ConfigurationEntry.SERVER_CONNECT_PASSWORD
                .removeValue(getController());
        }

        // Store new username/pw
        getController().saveConfig();
    }

    private void changeToServer(ServerInfo newServerInfo) {
        logFine("Changing server to " + newServerInfo.getNode());

        // Add key of new server to keystore.
        if (ProUtil.isRunningProVersion()
            && ProUtil.getPublicKey(getController(), newServerInfo.getNode()) == null)
        {
            try {
                PublicKey serverKey = publicKeyService
                    .getPublicKey(newServerInfo.getNode());
                if (serverKey != null) {
                    logFine("Retrieved new key for server "
                        + newServerInfo.getNode() + ". " + serverKey);
                    ProUtil.addNodeToKeyStore(getController(),
                        newServerInfo.getNode(), serverKey);
                }
            } catch (RuntimeException e) {
                logWarning("Not changing server. Unable to retrieve new server key for "
                    + newServerInfo.getName() + ". " + e);
                return;
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
        // Attempt to login. At least remind login for real connect.
        if (!isConnected()) {
            // Mark new server for connect
            server.markForImmediateConnect();
            Waiter w = new Waiter(1000);
            while (!w.isTimeout() && !isConnected()) {
                w.waitABit();
            }
            if (isConnected() && isFine()) {
                logFine("Connect success to " + server.getNick());
            }
        }
        login(username, passwordObf, true);
    }

    private void fireLogin(AccountDetails details) {
        fireLogin(details, true);
    }

    private void fireLogin(AccountDetails details, boolean loginSuccess) {
        listenerSupport
            .login(new ServerClientEvent(this, details, loginSuccess));
    }

    private void fireAccountUpdates(AccountDetails details) {
        listenerSupport.accountUpdated(new ServerClientEvent(this, details));
    }

    // General ****************************************************************

    public boolean showServerInfo() {
        if (getController().getDistribution().isBrandedClient()) {
            return false;
        }
        boolean pfCom = AbstractDistribution
            .isPowerFolderServer(getController());
        boolean prompt = ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM
            .getValueBoolean(getController());
        return prompt || !pfCom;
    }

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
                if (addr != null) {
                    if (addr.getAddress() != null) {
                        addrStr = NetworkUtil.getHostAddressNoResolve(addr
                            .getAddress());
                    } else {
                        addrStr = addr.getHostName();
                    }
                } else {
                    addrStr = "";
                }

                if (addr != null
                    && addr.getPort() != ConnectionListener.DEFAULT_PORT)
                {
                    addrStr += ":" + addr.getPort();
                }
            }
        } else {
            addrStr = "";
        }
        if (hasWebURL()) {
            return getWebURL();
        } else if (StringUtils.isNotBlank(addrStr)) {
            return "pf://" + addrStr;
        } else {
            return "n/a";
        }

    }

    @Override
    public String toString() {
        return "ServerClient "
            + (username != null ? username : "?")
            + "@"
            + (server != null ? server.getNick() + "("
                + server.getReconnectAddress() + ")" : "n/a");
    }

    // Inner classes **********************************************************

    public void primaryServerConnected(Member newNode) {
        ConnectionHandler conHan = newNode.getPeer();
        Identity id = conHan != null ? conHan.getIdentity() : null;
        supportsQuickLogin = id != null && id.isSupportsQuickLogin();
        if (supportsQuickLogin) {
            if (isFiner()) {
                logFiner("Quick login at server supported");
            }
            primaryServerConnected0(newNode);
        } else {
            logFine("Quick login at server NOT supported. Using regular login");
        }
    }

    private void primaryServerConnected0(Member newNode) {
        // Our server member instance is a temporary one. Lets get real.
        if (isTempServerNode(server)) {
            // Got connect to server! Take his ID and name.
            Member oldServer = server;
            setNewServerNode(newNode);
            // Remove old temporary server entry without ID.
            getController().getNodeManager().removeNode(oldServer);
            if (updateConfig) {
                setServerInConfig(server.getInfo());
                getController().saveConfig();
            }
            logInfo("Got connect to server: " + server + " nodeid: "
                + server.getId());
        }

        listenerSupport.serverConnected(new ServerClientEvent(this, newNode));

        if (username != null && StringUtils.isNotBlank(passwordObf)) {
            try {
                login(username, passwordObf, true);
                scheduleConnectHostingServers();
            } catch (Exception ex) {
                logWarning("Unable to login. " + ex);
                logFine(ex);
            }
        }

        // #2425
        if (ConfigurationEntry.SYNC_AND_EXIT.getValueBoolean(getController())) {
            // Check after 60 seconds. Then every 10 secs
            getController().performFullSync();
            getController().exitAfterSync(60);
        }
    }

    /**
     * This listener violates the rule "Listener/Event usage". Reason: Even when
     * a ServerClient is a true core-component there might be multiple
     * ClientServer objects that dynamically change.
     * <p>
     * http://dev.powerfolder.com/projects/powerfolder/wiki/GeneralDevelopRules
     */
    private class MyNodeManagerListener extends NodeManagerAdapter {
        @Override
        public void settingsChanged(NodeManagerEvent e) {
            // Transition Member.setServer(true)
            if (e.getNode().isServer()) {
                logInfo("Discovered a new server of " + countServers()
                    + " in cluster: " + e.getNode().getNick() + " @ "
                    + e.getNode().getReconnectAddress());
            } else if (getMySelf().isServer()) {
                logInfo("Not longer member of cluster: "
                    + e.getNode().getNick() + " @ "
                    + e.getNode().getReconnectAddress());
            }
            listenerSupport.nodeServerStatusChanged(new ServerClientEvent(
                ServerClient.this, e.getNode()));
        }

        private int countServers() {
            int n = 0;
            for (Member node : getController().getNodeManager().getNodesAsCollection()) {
                if (node.isServer()) {
                    n++;
                }
            }
            return n;
        }

        @Override
        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().isServer() && !isConnected()) {
                findAlternativeServer();
            }
            // #2366: Checked from via serverConnected(Member)
            if (ServerClient.this == getController().getOSClient()
                && supportsQuickLogin)
            {
                return;
            }
            // For JUnit tests only;
            if (isPrimaryServer(e.getNode())) {
                primaryServerConnected0(e.getNode());
            }
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (isPrimaryServer(e.getNode())) {
                findAlternativeServer();
                // Invalidate account.
                setAnonAccount();
                listenerSupport.serverDisconnected(new ServerClientEvent(
                    ServerClient.this, e.getNode()));
            }
        }

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public void folderRemoved(FolderRepositoryEvent e) {
        }

        @Override
        public void folderCreated(FolderRepositoryEvent e) {
            if (!getController().isStarted()) {
                return;
            }
            retrieveAndConnectoClusterServers();
        }

        @Override
        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        @Override
        public void maintenanceFinished(FolderRepositoryEvent e) {
        }
    }

    private class ServerConnectTask extends TimerTask {
        @Override
        public void run() {
            if (isConnected()) {
                return;
            }
            if (server.isMySelf()) {
                // Don't connect to myself
                return;
            }
            boolean allowLAN2Internet = ConfigurationEntry.SERVER_CONNECT_FROM_LAN_TO_INTERNET
                .getValueBoolean(getController());
            if (!allowLAN2Internet && getController().isLanOnly()
                && !server.isOnLAN())
            {
                logFiner("NOT connecting to server: " + server
                    + ". Reason: Server not on LAN");
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

            if (server.isUnableToConnect()) {
                // Try to connect to any known server
                findAlternativeServer();
            }
        }
    }

    private class AutoLoginTask extends TimerTask {
        @Override
        public void run() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (!isConnected()) {
                        return;
                    }
                    if (isLoggingIn()) {
                        return;
                    }
                    // PFC-2368: Verify login by server too.
                    if (isLoggedIn() && securityService.isLoggedIn()) {
                        return;
                    }
                    if (username != null
                        && (StringUtils.isNotBlank(passwordObf) || (StringUtils
                            .isBlank(passwordObf) && ConfigurationEntry.KERBEROS_SSO_ENABLED
                            .getValueBoolean(getController()))))
                    {
                        logInfo("Auto-Login: Logging in " + username);
                        login(username, passwordObf, true);
                    }
                }
            };
            getController().getIOProvider().startIO(r);
        }
    }

    /**
     * Task to retrieve hosting Online Storage servers which host my files.
     */
    private class HostingServersConnector extends TimerTask {
        @Override
        public void run() {
            connectHostingServers();
        }
    }

    private class MyThrowableHandler implements ThrowableHandler {
        private int loginProblems;

        @Override
        public void handle(Throwable t) {
            if (t instanceof NotLoggedInException) {
                autoLogin(t);
            } else if (t instanceof SecurityException) {
                if (t.getMessage() != null
                    && t.getMessage().toLowerCase().contains("not logged"))
                {
                    autoLogin(t);
                }
            }
        }

        private void autoLogin(Throwable t) {
            if (username != null && StringUtils.isNotBlank(passwordObf)) {
                loginProblems++;
                if (loginProblems > 20) {
                    logSevere("Got "
                        + loginProblems
                        + " login problems. "
                        + "Not longer auto-logging in to prevent hammering server.");
                    return;
                }
                logWarning("Auto-login for " + username
                    + " required. Caused by " + t);
                try {
                    login(username, passwordObf, true);
                } catch (Exception e) {
                    logWarning("Unable to login with " + username + " at "
                        + getServerString() + ". " + e);
                }
            }
        }
    }

    private class ServiceTicketGenerator implements
        PrivilegedExceptionAction<byte[]>
    {
        @Override
        public byte[] run() throws Exception {
            try {
            Oid kerberos5Oid = new Oid("1.2.840.113554.1.2.2");
            GSSManager gssManager = GSSManager.getInstance();
            GSSName clientName = gssManager.createName(username,
                GSSName.NT_USER_NAME);
            GSSName serviceName = gssManager.createName(
                ConfigurationEntry.KERBEROS_SSO_SERVICE_NAME
                    .getValue(getController())
                    + "@"
                    + ConfigurationEntry.KERBEROS_SSO_REALM
                        .getValue(getController()), null);

            GSSCredential clientCredentials = gssManager.createCredential(
                clientName, 8 * 60 * 60, kerberos5Oid,
                GSSCredential.INITIATE_ONLY);

            GSSContext gssContext = gssManager.createContext(serviceName,
                kerberos5Oid, clientCredentials, GSSContext.DEFAULT_LIFETIME);

            byte[] serviceTicket = gssContext.initSecContext(new byte[0], 0, 0);
            gssContext.dispose();
            return serviceTicket;
            } catch (Exception e) {
                logWarning(e);
                return null;
            }

        }
    }
}
