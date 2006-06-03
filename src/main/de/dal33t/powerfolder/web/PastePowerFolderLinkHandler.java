package de.dal33t.powerfolder.web;

import java.io.File;
import java.util.StringTokenizer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Util;

public class PastePowerFolderLinkHandler extends PFComponent implements
    Handler
{
    private static final String POWERFOLDER_LINK_PREFIX = "powerfolder://";
    public PastePowerFolderLinkHandler(Controller controller) {
        super(controller);
    }
        
    public HTTPResponse getPage(HTTPRequest httpRequest) {
        String message;
        if (httpRequest.getQueryParams().containsKey("pastelink")
            && httpRequest.getQueryParams().containsKey("SyncProfile"))
        {
            String link = httpRequest.getQueryParams().get("pastelink");            
            SyncProfile profile = createProfile(httpRequest.getQueryParams()
                .get("SyncProfile"));
            if (profile != null) {
                if (link.toLowerCase().startsWith(POWERFOLDER_LINK_PREFIX)) {
                    String plainLink = link.substring(POWERFOLDER_LINK_PREFIX
                        .length());                    

                    // Chop off ending /
                    if (plainLink.endsWith("/")) {
                        plainLink = plainLink.substring(1,
                            plainLink.length() - 1);
                    }
                    FolderRepository repo = getController()
                        .getFolderRepository();
                    // Parse link
                    StringTokenizer nizer = new StringTokenizer(plainLink, "|");
                    // Get type
                    String type = nizer.nextToken().toLowerCase();

                    if ("folder".equalsIgnoreCase(type)) {
                        // Decode the url form
                        String name = Util.decodeFromURL(nizer.nextToken());
                        boolean secret = nizer.nextToken()
                            .equalsIgnoreCase("s");
                        String id = Util.decodeFromURL(nizer.nextToken());
                        FolderInfo folder = new FolderInfo(name, id, secret);
                        String localDir = getController().getFolderRepository()
                            .getFoldersBasedir()
                            + System.getProperty("file.separator")
                            + Util.removeInvalidFilenameChars(name);
                        try {
                            repo.createFolderAsynchron(folder, new File(
                                localDir), profile, false);
                           message = "folder " + folder.name
                                + " created";
                        } catch (FolderException fe) {
                            message = 
                                "Folder create failed because: "
                                    + fe.getMessage();
                        }
                    } else {
                        message = "Not a valid PowerFolder link: "
                            + link;
                    }
                } else {
                    message = "Not a valid PowerFolder link: "
                        + link;
                }

            } else {
                message = "Profile error";
            }
        } else {
            message = "Invalid request, missing GET data";
        }
        return new HTTPResponse(message);
    }

    private SyncProfile createProfile(String id) {
        try {
            int profile = Integer.parseInt(id);
            switch (profile) {
                case 1 : { // Manual Download
                    return SyncProfile.MANUAL_DOWNLOAD;
                }
                case 2 : { // Auto download (from friends)
                    return SyncProfile.AUTO_DOWNLOAD_FROM_FRIENDS;
                }
                case 3 : { // Auto download (from everyone)
                    return SyncProfile.AUTO_DOWNLOAD_FROM_ALL;
                }
                case 4 : { // Synchronize PCs
                    return SyncProfile.SYNCHRONIZE_PCS;
                }
                case 5 : { // Project Work
                    return SyncProfile.PROJECT_WORK;
                }
                case 6 : { // Torrent Downloader
                    return SyncProfile.LEECHER;
                }
                case 7 : {// Torrent Releaser
                    return SyncProfile.LEECH_RELEASER;
                }
                default :
                    return null;
            }
        } catch (NumberFormatException nfe) {
            log().debug("profile creation failed: " + nfe);
            return null;
        }
    }
}
