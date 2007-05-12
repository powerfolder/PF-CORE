package de.dal33t.powerfolder.webservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;

/**
 * Low-level client for the webservice.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceClient extends PFComponent {
    public static final String SETUP_FOLDER_URL_SUFFIX = "/setupfolder";
    private static final String PROP_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private URL serviceURL;

    public WebServiceClient(Controller controller) {
        this(controller, Constants.WEBSERVICE_URL);
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
     * @return true if any webservice is connected. false if not.
     */
    public boolean isAWebServiceConnected() {
        List<Member> nodes = getController().getNodeManager()
            .getConnectedNodes();
        for (Member node : nodes) {
            if (isWebService(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the folder up to be mirrored by the webservice
     * 
     * @param folder
     *            the folder to be mirrored
     * @return true if succeeded
     */
    public boolean setupFolder(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        try {
            log().warn("Mirroing folder " + folder);

            // TODO Read user/pw from Config. Add encondigs
            URL setupFolderURL = new URL(serviceURL.toExternalForm()
                + SETUP_FOLDER_URL_SUFFIX
                + "?Username=sprajc@gmx.de&Password=qwertz");

            URLConnection con = setupFolderURL.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty(PROP_CONTENT_TYPE, CONTENT_TYPE);
            con.connect();
            ObjectOutputStream oOut = new ObjectOutputStream(con
                .getOutputStream());
            oOut.writeObject(folder.createInvitation());
            oOut.close();
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            StreamUtils.copyToStream(con.getInputStream(), bOut);
            String result = new String(bOut.toByteArray(), "UTF-8");
            con.getInputStream().close();

            log().warn("Result: " + result);
            return true;
        } catch (IOException e) {
            log().error("Unable to mirror folder: " + folder, e);
            return false;
        }
    }

    /**
     * @param folder
     *            the folder to check.
     * @return true if this folder gets mirrored by the webservice
     */
    public boolean isMirrored(Folder folder) {
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
            || node.getId().toLowerCase().contains("galactica");
    }

}
