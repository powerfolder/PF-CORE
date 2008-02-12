package de.dal33t.powerfolder.webservice;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.Reject;

/**
 * Low-level client for the webservice.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceClient extends PFComponent {
    public static final String SETUP_FOLDER_URL_SUFFIX = "setupfolder";
    public static final String TEST_LOGIN_URL_SUFFIX = "testlogin";
    private static final String PROP_CONTENT_TYPE = "Content-Type";
    private URL serviceURL;
    private boolean lastLoginOK;
    private boolean tringToConnect;

    public WebServiceClient(Controller controller) {
        this(controller, Constants.ONLINE_STORAGE_URL);
        this.lastLoginOK = false;
    }

    public WebServiceClient(Controller controller, String webServiceURL) {
        super(controller);
        Reject.ifNull(webServiceURL, "WebService is null");
        try {
            serviceURL = new URL(webServiceURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Webservice url is invalid: "
                + webServiceURL, e);
        }
    }

    /**
     * @return true if the account data has been set
     */
    public boolean isAccountSet() {
        return !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()))
            && !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
                .getValue(getController()));
    }

    /**
     * @return true if the last attempt to login to the online storage was ok.
     *         false if not or no login tried yet.
     */
    public boolean isLastLoginOK() {
        return lastLoginOK;
    }

    public void start() {
        getController().scheduleAndRepeat(new OnlineStorageConnectTask(), 0,
            1000L * 20);
    }

    /**
     * Checks a account against the powerfolder webservice.
     * 
     * @param username
     *            the username
     * @param password
     *            the password
     * @return true if the account is valid.
     */
    public boolean checkLogin(String username, String password) {
        lastLoginOK = false;
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return false;
        }
        try {
            log().warn("Check login (" + username + "/" + password + ")");
            WebServiceResponse response = executeRequest(username, password,
                TEST_LOGIN_URL_SUFFIX, null);
            log().warn("Result: " + response);
            if (!response.isFailure()) {
                lastLoginOK = true;
                return true;
            }
            Throwable t = (Throwable) response.getValue();
            log().error("Login failed", t);
            return false;
        } catch (IOException e) {
            log().error("Login failed", e);
        } catch (ClassNotFoundException e) {
            log().error("Login failed", e);
        }
        return false;
    }

    /**
     * Sets the folder up to be mirrored by the webservice
     * 
     * @param folder
     *            the folder to be mirrored
     * @throws WebServiceException
     *             if something went wrong. exception text contains explaination
     */
    public void setupFolder(Folder folder) throws WebServiceException {
        Reject.ifNull(folder, "Folder is null");
        Reject.ifBlank(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()), "WebService account not setup");
        try {
            log().warn("Mirroing folder " + folder);
            WebServiceResponse response = executeRequest(
                SETUP_FOLDER_URL_SUFFIX, folder.createInvitation());

            log().warn("Result: " + response);
            if (response.isFailure()) {
                Throwable t = (Throwable) response.getValue();
                throw new WebServiceException(t.getMessage());
            }
        } catch (IOException e) {
            throw new WebServiceException("Unable to mirror folder", e);
        } catch (ClassNotFoundException e) {
            throw new WebServiceException("Unable to mirror folder", e);
        }
    }

    /**
     * @return true if any webservice is connected. false if not.
     */
    public boolean isAWebServiceConnected() {
        return getAConnectedWebService() != null;
    }

    /**
     * @return the first found web service.
     */
    public Member getAConnectedWebService() {
        Collection<Member> nodes = getController().getNodeManager()
            .getConnectedNodes();
        for (Member node : nodes) {
            if (node.isCompleteyConnected() && isWebService(node)) {
                return node;
            }
        }
        return null;
    }

    /**
     * @param folder
     *            the folder to check.
     * @return true if this folder gets mirrored by the webservice
     */
    private boolean isMirrored(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        for (Member member : folder.getMembers()) {
            if (isWebService(member)) {
                // FIXME: Check if the webservice is running in auto-download
                // mode.
                return true;
            }
        }
        return false;
    }

    /**
     * @return the mirrored folders by the webservice.
     */
    public List<Folder> getMirroredFolders() {
        List<Folder> mirroredFolders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (isMirrored(folder)) {
                mirroredFolders.add(folder);
            }
        }
        return mirroredFolders;
    }

    /**
     * @param node
     *            the node to check
     * @return true if the node is a webservice
     */
    public boolean isWebService(Member node) {
        // TODO Create a better way to detect that.
        return node.getId().toLowerCase().contains("webservice")
            || node.getId().toLowerCase().contains("galactica")
            || node.getId().toLowerCase().contains("onlinestorage");
    }

    // Low-level RPC stuff ****************************************************

    private WebServiceResponse executeRequest(String action,
        Serializable request) throws IOException, ClassNotFoundException
    {
        return executeRequest(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()), ConfigurationEntry.WEBSERVICE_PASSWORD
            .getValue(getController()), action, request);
    }

    private WebServiceResponse executeRequest(String username, String password,
        String action, Serializable request) throws IOException,
        ClassNotFoundException
    {
        ObjectOutputStream oOut = null;
        ObjectInputStream oIn = null;
        try {
            URL requestURL = buildRequestURL(username, password, action);
            URLConnection con = requestURL.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty(PROP_CONTENT_TYPE,
                WebServiceResponse.CONTENT_TYPE);
            // con.connect();
            oOut = new ObjectOutputStream(con.getOutputStream());
            log().warn("Writing object:" + request);
            oOut.writeObject(request);
            oIn = new ObjectInputStream(new GZIPInputStream(con
                .getInputStream()));
            return (WebServiceResponse) oIn.readObject();
        } finally {
            if (oIn != null) {
                oIn.close();
            }
            if (oOut != null) {
                oOut.close();
            }
        }
    }

    private URL buildRequestURL(String username, String password, String action)
        throws UnsupportedEncodingException, MalformedURLException
    {
        URL requestURL = new URL(serviceURL.toExternalForm() + "/" + action
            + "?Username=" + URLEncoder.encode(username, "UTF-8")
            + "&Password=" + URLEncoder.encode(password, "UTF-8"));
        return requestURL;
    }

    // Internal classes *******************************************************

    private class OnlineStorageConnectTask extends TimerTask {
        @Override
        public void run() {
            if (isAWebServiceConnected()) {
                return;
            }
            if (isWebService(getController().getMySelf())) {
                return;
            }
            if (tringToConnect) {
                return;
            }
            if (getController().isLanOnly()) {
                return;
            }
            if (!ConfigurationEntry.AUTO_CONNECT
                .getValueBoolean(getController()))
            {
                return;
            }
            tringToConnect = true;
            Runnable connector = new Runnable() {
                public void run() {
                    try {
                        log().debug(
                            "Triing to connect to Online Storage ("
                                + Constants.ONLINE_STORAGE_ADDRESS + ")");
                        ConnectionHandler conHan = getController()
                            .getIOProvider().getConnectionHandlerFactory()
                            .tryToConnect(Constants.ONLINE_STORAGE_ADDRESS);
                        getController().getNodeManager().acceptConnection(
                            conHan);
                    } catch (ConnectionException e) {
                        log().warn("Unable to connect to online storage", e);
                    } finally {
                        tringToConnect = false;
                    }
                }
            };
            getController().getIOProvider().startIO(connector);
        }
    }
}
