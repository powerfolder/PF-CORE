package de.dal33t.powerfolder.web;

import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;

public class LeaveFolderHandler extends PFComponent implements Handler {

    public LeaveFolderHandler(Controller controller) {
        super(controller);
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {
        FolderRepository repo = getController().getFolderRepository();
        List<Folder> folders = repo.getFoldersAsSortedList();
        Map<String, String> params = httpRequest.getQueryParams();
        String message = null;
        if (params != null && params.containsKey("leave")
            && params.containsKey("ID") && params.get("leave").equals("true"))
        {
            // leave folder
            for (Folder folder : folders) {
                if (folder.getId().equals(params.get("ID"))) {
                    repo.removeFolder(folder);                    
                    message = "left folder: " + folder.getName();                    
                }
            }
            if (message == null) {
                message = "folder not found";
            }

        } else {
            message = "invalid request";
        }
        return new HTTPResponse(message.getBytes());
    }
}