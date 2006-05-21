package de.dal33t.powerfolder.web;

import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.Translation;

public class SetSyncProfileHandler extends PFComponent implements Handler {

    public SetSyncProfileHandler(Controller controller) {
        super(controller);
    }
   
    public HTTPResponse getPage(HTTPRequest httpRequest)
    {
        FolderRepository repo = getController().getFolderRepository();
        List<Folder> folders = repo.getFoldersAsSortedList();
        Map<String, String> params = httpRequest.getQueryParams();
        String profileID = params.get("SyncProfileID");
        SyncProfile profile = SyncProfile.getSyncProfileById(profileID);
        String message = null;
        if (profile != null) {
            //if (params.get("setProfile").equals("Save")) {
                // set new Syncprofile
                boolean folderFound = false;
                for (Folder folder : folders) {    
                    log().debug(folder.getId() + "=" + params.get("FolderID") + "?");
                    if (folder.getId().equals(params.get("FolderID"))) {
                        log().debug("folderfound");
                        folderFound = true;
                        message = "set profile of folder: "
                            + folder.getName() + " to " + Translation.getTranslation(profile.getTranslationId()) + " succeded.";
                        folder.setSyncProfile(profile);
                        break;
                    }
                    
                }
                if (!folderFound) {
                    message = "folder not found: " + params.get("FolderID");
                }
            //} else {
            //    message = "setprofile != 'Save'";
            //}
        } else {
            message = "profile == null";
        }    
        return new HTTPResponse(message.getBytes());
    }
}
