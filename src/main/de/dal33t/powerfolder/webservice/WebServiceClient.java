package de.dal33t.powerfolder.webservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Reject;

/**
 * Low-level client for the webservice.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceClient extends PFComponent {

    public WebServiceClient(Controller controller) {
        super(controller);
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

    public boolean mirrorFolder(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        log().warn("Mirroing folder " + folder);
        return true;
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
    public Collection<Folder> getMirroredFolders() {
        Collection<Folder> mirroredFolders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (isMirrored(folder)) {
                mirroredFolders.add(folder);
            }
        }
        return mirroredFolders;
    }

    private boolean isWebService(Member node) {
        // TODO Create a better way to detect that.
        return node.getId().toLowerCase().contains("webservice")
            || node.getId().toLowerCase().contains("galactica");
    }

}
